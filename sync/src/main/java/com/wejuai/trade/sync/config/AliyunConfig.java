package com.wejuai.trade.sync.config;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.wejuai.trade.sync.service.TradeService;
import com.wejuai.trade.sync.support.AliyunMnsTradeEventSubscriber;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;


/**
 * @author YQ.Huang
 */
@Configuration
@EnableConfigurationProperties({
        AliyunConfig.Properties.class,
        AliyunConfig.MnsProperties.class})
public class AliyunConfig {

    public final Properties aliyun;
    public final MnsProperties mns;

    public AliyunConfig(Properties aliyun, MnsProperties mns) {
        this.aliyun = aliyun;
        this.mns = mns;
    }

    @Bean
    CloudQueue chargeStartedQueue() {
        CloudAccount cloudAccount = new CloudAccount(aliyun.getAccessKeyId(), aliyun.getAccessKeySecret(), mns.getEndpoint());
        MNSClient mnsClient = cloudAccount.getMNSClient();
        return mnsClient.getQueueRef(mns.getName());
    }

    @Bean
    AliyunMnsTradeEventSubscriber aliyunMnsTradeEventSubscriber(CloudQueue chargeStartedQueue, TradeService tradeService) {
        return new AliyunMnsTradeEventSubscriber(chargeStartedQueue, tradeService);
    }

    @Validated
    @ConfigurationProperties(prefix = "aliyun")
    public static class Properties {

        @NotBlank
        private String accessKeyId;
        @NotBlank
        private String accessKeySecret;

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }
    }

    @Validated
    @ConfigurationProperties(prefix = "aliyun.mns")
    public static class MnsProperties {
        @NotBlank
        private String name;
        @NotBlank
        private String endpoint;

        public String getName() {
            return name;
        }

        public MnsProperties setName(String name) {
            this.name = name;
            return this;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public MnsProperties setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
    }
}