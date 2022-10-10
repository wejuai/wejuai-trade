package com.wejuai.trade.core.config;

import com.endofmaster.weixin.pay.WxPayApi;
import com.wejuai.alipay.AlipayClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import java.io.InputStream;

/**
 * @author ZM.Wang
 */
@Configuration
@EnableConfigurationProperties({
        TradeConfig.AlipayProperties.class,
        TradeConfig.WxpayProperties.class,
        TradeConfig.WxpayMiniAppProperties.class,
        TradeConfig.WxpayOffiProperties.class})
public class TradeConfig {

    public final AlipayProperties alipay;
    public final WxpayProperties weixin;
    /** 小程序参数 */
    public final WxpayMiniAppProperties miniApp;
    /** 公众号参数 */
    public final WxpayOffiProperties offi;

    public TradeConfig(AlipayProperties alipay, WxpayProperties weixin, WxpayMiniAppProperties miniApp, WxpayOffiProperties offi) {
        this.alipay = alipay;
        this.weixin = weixin;
        this.miniApp = miniApp;
        this.offi = offi;
    }

    @Bean
    AlipayClient alipayClient() {
        return new AlipayClient(alipay.getId(), alipay.getKey(), alipay.getAppCertSn());
    }

    @Bean
    WxPayApi wxMiniAppPayApi() {
        InputStream cert = this.getClass().getResourceAsStream("/weixin.p12");
        return new WxPayApi(weixin.getKey(), miniApp.getAppId(), weixin.getMchId(), cert, weixin.getMchId());
    }

    @Bean
    WxPayApi wxOffiPayApi() {
        InputStream cert = this.getClass().getResourceAsStream("/weixin.p12");
        return new WxPayApi(weixin.getKey(), offi.getAppId(), weixin.getMchId(), cert, weixin.getMchId());
    }

    @Validated
    @ConfigurationProperties(prefix = "trade.alipay")
    public static class AlipayProperties {

        @NotBlank
        private String id;

        @NotBlank
        private String key;

        @NotBlank
        private String appCertSn;

        @NotBlank
        private String notifyUrl;

        public String getId() {
            return id;
        }

        public String getKey() {
            return key;
        }

        public AlipayProperties setId(String id) {
            this.id = id;
            return this;
        }

        public AlipayProperties setKey(String key) {
            this.key = key;
            return this;
        }

        public String getAppCertSn() {
            return appCertSn;
        }

        public AlipayProperties setAppCertSn(String appCertSn) {
            this.appCertSn = appCertSn;
            return this;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public AlipayProperties setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "trade.weixin")
    public static class WxpayProperties {

        @NotBlank
        private String mchId;

        private String key;

        @NotBlank
        private String notifyUrl;

        public String getMchId() {
            return mchId;
        }

        public WxpayProperties setMchId(String mchId) {
            this.mchId = mchId;
            return this;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public WxpayProperties setNotifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        public String getKey() {
            return key;
        }

        public WxpayProperties setKey(String key) {
            this.key = key;
            return this;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "trade.weixin.mini-app")
    public static class WxpayMiniAppProperties {

        @NotBlank
        private String appId;

        @NotBlank
        private String key;

        public String getAppId() {
            return appId;
        }

        public WxpayMiniAppProperties setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public String getKey() {
            return key;
        }

        public WxpayMiniAppProperties setKey(String key) {
            this.key = key;
            return this;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "trade.weixin.offi")
    public static class WxpayOffiProperties {

        @NotBlank
        private String appId;

        @NotBlank
        private String key;

        public String getAppId() {
            return appId;
        }

        public WxpayOffiProperties setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public String getKey() {
            return key;
        }

        public WxpayOffiProperties setKey(String key) {
            this.key = key;
            return this;
        }
    }

    public String getMchKey() {
        return weixin.getKey();
    }

    public String getOffiAppId() {
        return offi.getAppId();
    }

    public String getMiniAppId() {
        return miniApp.getAppId();
    }

    public String getWxpayNotifyUrl() {
        return weixin.getNotifyUrl();
    }

    public String getAlipayNotifyUrl() {
        return alipay.getNotifyUrl();
    }
}
