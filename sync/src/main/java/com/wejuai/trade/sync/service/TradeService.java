package com.wejuai.trade.sync.service;

import com.endofmaster.weixin.pay.WxPayApi;
import com.endofmaster.weixin.pay.WxpayChargeCallback;
import com.endofmaster.weixin.pay.WxpayChargeQueryRequest;
import com.endofmaster.weixin.pay.WxpayChargeQueryResponse;
import com.wejuai.alipay.AlipayClient;
import com.wejuai.alipay.query.AlipayChargeCallback;
import com.wejuai.alipay.query.AlipayChargeQueryRequest;
import com.wejuai.alipay.query.AlipayChargeQueryResponse;
import com.wejuai.entity.mongo.statistics.ChargeStatisticsType;
import com.wejuai.entity.mongo.trade.Charge;
import com.wejuai.entity.mongo.trade.TradeStatus;
import com.wejuai.entity.mysql.ChannelType;
import com.wejuai.entity.mysql.Orders;
import com.wejuai.entity.mysql.User;
import com.wejuai.trade.core.config.TradeConfig;
import com.wejuai.trade.core.repository.ChargeRepository;
import com.wejuai.trade.core.service.StatisticsService;
import com.wejuai.trade.sync.repository.OrdersRepository;
import com.wejuai.trade.sync.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * @author ZM.Wang
 */
@Service
public class TradeService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ChargeRepository chargeRepository;
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final AlipayClient alipayClient;
    private final WxPayApi wxOffiPayApi;
    private final WxPayApi wxMiniAppPayApi;
    private final TradeConfig tradeConfig;
    private final StatisticsService statisticsService;

    public TradeService(ChargeRepository chargeRepository, AlipayClient alipayClient, OrdersRepository ordersRepository, UserRepository userRepository, WxPayApi wxOffiPayApi, WxPayApi wxMiniAppPayApi, TradeConfig tradeConfig, StatisticsService statisticsService) {
        this.chargeRepository = chargeRepository;
        this.alipayClient = alipayClient;
        this.ordersRepository = ordersRepository;
        this.userRepository = userRepository;
        this.wxOffiPayApi = wxOffiPayApi;
        this.wxMiniAppPayApi = wxMiniAppPayApi;
        this.tradeConfig = tradeConfig;
        this.statisticsService = statisticsService;
    }

    @Transactional
    public void handleChargeStartedEvent(String chargeId) {
        Optional<Charge> chargeOptional = chargeRepository.findById(chargeId);
        if (!chargeOptional.isPresent()) {
            logger.warn("忽略[ChargeStartedEvent], {}不存在", chargeId);
            return;
        }
        Charge charge = chargeOptional.get();
        if (charge.getStatus() == TradeStatus.SUCCEEDED) {
            logger.warn("忽略[ChargeStartedEvent], {}已完成", chargeId);
            return;
        }
        ChannelType channelType = charge.getChannelType();
        if (channelType == ChannelType.ALIPAY_WEB || channelType == ChannelType.ALIPAY_WAP) {
            AlipayChargeQueryRequest request = new AlipayChargeQueryRequest(charge.getReturnUrl(), charge.getNotifyUrl(), chargeId);
            AlipayChargeQueryResponse response = alipayClient.execute(request);
            if (response.getChargeStatus()) {
                charge.complete(true, null);
                charge.setChanTradeNo(response.getTradeNo());
                addUserIntegral(charge);
                chargeRepository.save(charge);
            }
        } else if (channelType == ChannelType.WEIXIN_H5 || channelType == ChannelType.WEIXIN_JSAPI || channelType == ChannelType.WEIXIN_PC) {
            wxChargeHandle(chargeId, charge, wxOffiPayApi);
        } else if (channelType == ChannelType.WEIXIN_MINI_APP) {
            wxChargeHandle(chargeId, charge, wxMiniAppPayApi);
        }
    }

    @Transactional
    public void alipayNotify(String chargeId, HttpServletRequest request, HttpServletResponse response) {
        Optional<Charge> chargeOptional = chargeRepository.findById(chargeId);
        if (!chargeOptional.isPresent()) {
            logger.error("没有该支付宝订单：" + chargeId);
            return;
        }
        Charge charge = chargeOptional.get();
        if (charge.getStatus() == TradeStatus.SUCCEEDED) {
            logger.warn("已成功订单收到回调信息:" + chargeId);
            return;
        }
        AlipayChargeCallback chargeCallback = new AlipayChargeCallback(request);
        chargeCallback.validate(chargeId, charge.getAmount());
        chargeCallbackHandle(charge, chargeCallback.getTradeNo());
        chargeCallback.ack(response);
    }

    @Transactional
    public void weixinNotify(String chargeId, HttpServletRequest request, HttpServletResponse response) {
        Optional<Charge> chargeOptional = chargeRepository.findById(chargeId);
        if (!chargeOptional.isPresent()) {
            logger.error("没有该微信订单：" + chargeId);
            return;
        }
        Charge charge = chargeOptional.get();
        if (charge.getStatus() == TradeStatus.SUCCEEDED) {
            logger.warn("已成功订单收到回调信息:" + chargeId);
            return;
        }
        WxpayChargeCallback chargeCallback = new WxpayChargeCallback(tradeConfig.getMchKey(), request);
        chargeCallback.validate(charge.getAmount());
        chargeCallbackHandle(charge, chargeCallback.getTradeNo());
        chargeCallback.ack(response);
    }

    @Transactional
    void addUserIntegral(Charge charge) {
        Optional<User> userOptional = userRepository.findById(charge.getUserId());
        if (!userOptional.isPresent()) {
            logger.warn("未找到订单中用户id，chargeId：{}，userId：{}", charge.getId(), charge.getUserId());
            return;
        }
        User user = userOptional.get();
        userRepository.save(user.addIntegral(charge.getAmount()));
        ordersRepository.save(new Orders(charge, user));
    }

    private void chargeCallbackHandle(Charge charge, String channelTradeId) {
        charge.setChanTradeNo(channelTradeId);
        Optional<User> userOptional = userRepository.findById(charge.getUserId());
        if (!userOptional.isPresent()) {
            logger.warn("未找到订单中用户id，chargeId：{}，userId：{}", charge.getId(), charge.getUserId());
            return;
        }
        addUserIntegral(charge);
        charge.complete(true, null);
        chargeRepository.save(charge);
        new Thread(() -> statisticsService.addCharge(ChargeStatisticsType.RECHARGE, charge.getAmount()));
    }

    private void wxChargeHandle(String chargeId, Charge charge, WxPayApi wxPayApi) {
        WxpayChargeQueryRequest request = new WxpayChargeQueryRequest(chargeId);
        WxpayChargeQueryResponse response = wxPayApi.execute(request);
        if (response.getChargeStatus()) {
            charge.complete(true, null);
            charge.setChanTradeNo(response.getTransactionId());
            addUserIntegral(charge);
            chargeRepository.save(charge);
        }
    }
}
