package com.wejuai.trade.gateway.support;

import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.model.Message;
import com.wejuai.entity.mongo.trade.Charge;
import com.wejuai.exception.TradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author YQ.Huang
 */
public class AliyunMnsTradeEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AliyunMnsTradeEventPublisher.class);

    private final CloudQueue chargeStartedQueue;

    public AliyunMnsTradeEventPublisher(CloudQueue chargeStartedQueue) {
        this.chargeStartedQueue = chargeStartedQueue;
    }

    public void publishChargeStarted(Charge charge) {
        try {
            Message message = new Message(charge.getId());
            message.setDelaySeconds(60);
            chargeStartedQueue.putMessage(message);
            logger.debug("发布[ChargeStarted]成功, {}", charge);
        } catch (Exception e) {
            logger.error("发布[ChargeStarted]失败, {}", charge);
            throw new TradeException(e);
        }
    }

}
