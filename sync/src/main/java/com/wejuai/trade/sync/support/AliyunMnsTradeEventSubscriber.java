package com.wejuai.trade.sync.support;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.model.Message;
import com.wejuai.trade.sync.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 交易查询服务，负责在用户发起支付和退款后主动跟踪（查询）它们的状态
 *
 * @author YQ.Huang
 */
public class AliyunMnsTradeEventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(AliyunMnsTradeEventSubscriber.class);

    private final CloudQueue chargeStartedQueue;
    private final TradeService tradeService;

    public AliyunMnsTradeEventSubscriber(CloudQueue chargeStartedQueue, TradeService tradeService) {
        this.chargeStartedQueue = chargeStartedQueue;
        this.tradeService = tradeService;
    }

    @Scheduled(fixedDelay = 30 * 1000)
    public void scheduledReceiveChargeStartedMessages() {
        List<Message> messages = receiveChargeStartedMessages();
        if (messages != null && !messages.isEmpty()) {
            logger.debug("拉取到[{}]条[ChargeStarted]消息", messages.size());
            for (Message message : messages) {
                try {
                    String chargeId = message.getMessageBody();
                    tradeService.handleChargeStartedEvent(chargeId);
                    deleteChargeStartedMessage(message);
                    logger.debug("处理[ChargeStarted]消息成功");
                } catch (Exception e) {
                    logger.error("处理[ChargeStarted]消息失败", e);
                }
            }
        }
    }

    private List<Message> receiveChargeStartedMessages() {
        try {
            return chargeStartedQueue.batchPopMessage(16);
        } catch (Exception e) {
            logger.error("拉取[ChargeStarted]消息失败", e);
            return null;
        }
    }

    private void deleteChargeStartedMessage(Message message) {
        chargeStartedQueue.deleteMessage(message.getReceiptHandle());
        logger.debug("删除[ChargeStarted]消息成功");
    }

}
