package com.solace.samples.spring.boot.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

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
    private String topicName;
    private Queues queues;

    @Data
    public static class Queues {
        private Queue queue;
    }

    @Data
    public static class Queue {
        private String name;
        private List<String> subscriptions;
    }
}
