package com.solace.samples.spring.boot.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "solace")
@Getter
@Setter
public class SolaceConfigProperties {
    private String hostUrl;
    private String vpnName;
    private String userName;
    private String password;
    private String reconnectionAttempts;
    private String connectionRetriesPerHost;

    private QueueConfig requestQueue;
    private QueueConfig replyQueue;
    private String requestTopic;
    private long replyTimeoutMs;

    @Data
    public static class QueueConfig {
        private String name;
        private String subscription;
    }
}
