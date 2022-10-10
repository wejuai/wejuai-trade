package com.wejuai.trade.gateway.service;

import com.endofmaster.commons.id.IdGenerator;
import com.endofmaster.rest.exception.BadRequestException;
import com.endofmaster.weixin.WxException;
import com.endofmaster.weixin.pay.WxMerchantPayRequest;
import com.endofmaster.weixin.pay.WxMerchantPayResponse;
import com.endofmaster.weixin.pay.WxPayApi;
import com.endofmaster.weixin.pay.WxPayNativeChargeRequest;
import com.endofmaster.weixin.pay.WxPayNativeChargeResponse;
import com.endofmaster.weixin.pay.WxpayH5ChargeRequest;
import com.endofmaster.weixin.pay.WxpayH5ChargeResponse;
import com.endofmaster.weixin.pay.WxpayJsapiChargeRequest;
import com.endofmaster.weixin.pay.WxpayJsapiChargeResponse;
import com.wejuai.alipay.AlipayClient;
import com.wejuai.alipay.direct.AlipayDirectChargeRequest;
import com.wejuai.alipay.transfer.AlipayTransferRequest;
import com.wejuai.alipay.transfer.AlipayTransferResponse;
import com.wejuai.alipay.wap.AlipayWapChargeRequest;
import com.wejuai.dto.request.RechargeRequest;
import com.wejuai.dto.request.WithdrawalTradeRequest;
import com.wejuai.dto.response.MchTradeResponse;
import com.wejuai.entity.mongo.MerchantTransfer;
import com.wejuai.entity.mongo.trade.Charge;
import com.wejuai.entity.mongo.trade.TradeStatus;
import com.wejuai.entity.mysql.ChannelType;
import com.wejuai.entity.mysql.WithdrawalType;
import com.wejuai.trade.core.config.TradeConfig;
import com.wejuai.trade.core.repository.ChargeRepository;
import com.wejuai.trade.core.service.StatisticsService;
import com.wejuai.trade.gateway.repository.MerchantTransferRepository;
import com.wejuai.trade.gateway.support.AliyunMnsTradeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import static com.endofmaster.weixin.Constant.CHARSET;
import static com.wejuai.entity.mongo.statistics.ChargeStatisticsType.TRANSFER;

/**
 * @author ZM.Wang
 */
@Service
public class OrderService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ChargeRepository chargeRepository;
    private final MerchantTransferRepository merchantTransferRepository;

    private final WxPayApi wxOffiPayApi;
    private final TradeConfig tradeConfig;
    private final WxPayApi wxMiniAppPayApi;
    private final AlipayClient alipayClient;
    private final AliyunMnsTradeEventPublisher aliyunMnsTradeEventPublisher;
    private final StatisticsService statisticsService;

    public OrderService(ChargeRepository chargeRepository, MerchantTransferRepository merchantTransferRepository, AlipayClient alipayClient, WxPayApi wxOffiPayApi, WxPayApi wxMiniAppPayApi, TradeConfig tradeConfig, AliyunMnsTradeEventPublisher aliyunMnsTradeEventPublisher, StatisticsService statisticsService) {
        this.chargeRepository = chargeRepository;
        this.merchantTransferRepository = merchantTransferRepository;
        this.alipayClient = alipayClient;
        this.wxOffiPayApi = wxOffiPayApi;
        this.wxMiniAppPayApi = wxMiniAppPayApi;
        this.tradeConfig = tradeConfig;
        this.aliyunMnsTradeEventPublisher = aliyunMnsTradeEventPublisher;
        this.statisticsService = statisticsService;
    }

    @Transactional
    public Map<String, String> payment(String userId, String ip, RechargeRequest request) {
        if (request.getType() == ChannelType.ALIPAY_WAP || request.getType() == ChannelType.ALIPAY_WEB) {
            return alipay(userId, request);
        } else {
            return weixin(userId, ip, request);
        }
    }

    @Transactional
    public MchTradeResponse merchantPay(WithdrawalTradeRequest request, String ip) {
        MerchantTransfer merchantTransfer;
        WithdrawalType channelType = request.getType();
        long amount = request.getAmount();
        if (channelType == WithdrawalType.ALIPAY) {
            merchantTransfer = aliMchPay(request.getUserId(), amount, request.getCardNo(), request.getName(), request.getDesc());
        } else if (channelType == WithdrawalType.WEIXIN) {
            merchantTransfer = wxMchPay(request.getUserId(), amount, request.getCardNo(), request.getDesc(), ip);
        } else {
            throw new BadRequestException("没有该类型提现方式: " + channelType);
        }
        new Thread(() -> statisticsService.addCharge(TRANSFER, amount));
        return new MchTradeResponse(merchantTransfer.getStatus() == TradeStatus.SUCCEEDED, merchantTransfer.getChannelTradeId(), merchantTransfer.getId());

    }

    private MerchantTransfer wxMchPay(String userId, long amount, String openId, String desc, String ip) {
        String chargeId = IdGenerator.objectId();
        WxMerchantPayRequest request = new WxMerchantPayRequest(chargeId, openId, amount, desc, ip);
        WxMerchantPayResponse response = wxMiniAppPayApi.execute(request);
        MerchantTransfer merchantTransfer = new MerchantTransfer(userId, amount, WithdrawalType.WEIXIN);
        if (!response.isResultSuccessful()) {
            merchantTransfer.complete(false, null, response.getErrorMsg());
            logger.error("微信转账失败: " + response.getErrorMsg());
            return merchantTransferRepository.save(merchantTransfer);
        }
        logger.debug("微信提现转账成功, 金额: " + amount);
        merchantTransfer.complete(true, response.getPaymentNo(), null);
        return merchantTransferRepository.save(merchantTransfer);
    }

    private MerchantTransfer aliMchPay(String userId, long amount, String cardNo, String name, String desc) {
        String chargeId = IdGenerator.objectId();
        AlipayTransferRequest request = new AlipayTransferRequest(chargeId, amount / 100D + "", desc, cardNo, name, "TRANS_ACCOUNT_NO_PWD");
        AlipayTransferResponse response = alipayClient.execute(request);
        MerchantTransfer merchantTransfer = new MerchantTransfer(userId, amount, WithdrawalType.ALIPAY);
        if (!response.isSuccessful()) {
            merchantTransfer.complete(false, null, response.getErrorMessage());
            logger.error("微信转账失败: " + response.getErrorMessage());
            return merchantTransferRepository.save(merchantTransfer);
        }
        logger.debug("支付宝提现转账成功, 金额(分): " + amount);
        merchantTransfer.complete(true, response.getOrderId(), null);
        return merchantTransferRepository.save(merchantTransfer);
    }

    private Map<String, String> alipay(String userId, RechargeRequest request) {
        String id = IdGenerator.objectId();
        String notifyUrl = tradeConfig.getAlipayNotifyUrl() + "/" + id;
        Charge charge = new Charge(id, userId, request.getAmount(), "为聚爱充值", request.getType(), request.getState(),
                notifyUrl);
        chargeRepository.save(charge);
        Map<String, String> params;
        if (request.getType() == ChannelType.ALIPAY_WEB) {
            AlipayDirectChargeRequest alipayReq = new AlipayDirectChargeRequest(charge.getId(), "为聚爱充值",
                    charge.getAmount() / 100.0 + "", request.getState(), notifyUrl);
            params = alipayClient.execute(alipayReq).buildCredentials();
        } else if (request.getType() == ChannelType.ALIPAY_WAP) {
            AlipayWapChargeRequest alipayReq = new AlipayWapChargeRequest(charge.getId(), "为聚爱充值",
                    charge.getAmount() / 100.0 + "", request.getState(), notifyUrl);
            params = alipayClient.execute(alipayReq).buildCredentials();
        } else {
            throw new BadRequestException("没有该支付通道");
        }
        aliyunMnsTradeEventPublisher.publishChargeStarted(charge);
        return params;
    }

    private Map<String, String> weixin(String userId, String ip, RechargeRequest request) {
        String id = IdGenerator.objectId();
        String notifyUrl = tradeConfig.getWxpayNotifyUrl() + "/" + id;
        Charge charge = new Charge(id, userId, request.getAmount(), "为聚爱充值", request.getType(), request.getState(), notifyUrl);
        chargeRepository.save(charge);
        Map<String, String> result;
        if (request.getType() == ChannelType.WEIXIN_PC) {
            WxPayNativeChargeRequest wxReq = new WxPayNativeChargeRequest(charge.getId(), "为聚爱充值", charge.getAmount() + "",
                    notifyUrl, charge.getId(), ip);
            WxPayNativeChargeResponse response = wxOffiPayApi.execute(wxReq);
            if (response.isResultSuccessful()) {
                result = Collections.singletonMap("qr", response.getCodeUrl());
            } else {
                logger.error("调用微信错误：" + response.getErrorMsg());
                throw new WxException("调用微信支付错误");
            }
        } else if (request.getType() == ChannelType.WEIXIN_JSAPI) {
            WxpayJsapiChargeRequest wxReq = new WxpayJsapiChargeRequest("为聚爱充值", id, request.getAmount(),
                    notifyUrl, request.getOpenId(), ip);
            WxpayJsapiChargeResponse response = wxOffiPayApi.execute(wxReq);
            result = response.buildCredentials(tradeConfig.getOffiAppId(), tradeConfig.getMchKey());
        } else if (request.getType() == ChannelType.WEIXIN_MINI_APP) {
            WxpayJsapiChargeRequest wxReq = new WxpayJsapiChargeRequest("为聚爱充值", id, request.getAmount(),
                    notifyUrl, request.getOpenId(), ip);
            WxpayJsapiChargeResponse response = wxMiniAppPayApi.execute(wxReq);
            result = response.buildCredentials(tradeConfig.getMiniAppId(), tradeConfig.getMchKey());
        } else if (request.getType() == ChannelType.WEIXIN_H5) {
            WxpayH5ChargeRequest wxReq = new WxpayH5ChargeRequest("为聚爱充值", id, request.getAmount() + "",
                    notifyUrl, ip, notifyUrl);
            WxpayH5ChargeResponse response = wxOffiPayApi.execute(wxReq);
            try {
                result = Collections.singletonMap("url", response.getUrl() + "&redirect_url=" + URLEncoder.encode(request.getState(), CHARSET));
            } catch (UnsupportedEncodingException e) {
                logger.warn("微信支付设置state urlEncoding失败", e);
                result = Collections.singletonMap("url", response.getUrl());
            }
        } else {
            throw new BadRequestException("没有该支付通道");
        }
        aliyunMnsTradeEventPublisher.publishChargeStarted(charge);
        return result;
    }

}
