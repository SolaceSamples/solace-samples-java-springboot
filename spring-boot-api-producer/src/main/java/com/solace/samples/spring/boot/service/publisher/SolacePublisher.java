package com.solace.samples.spring.boot.service.publisher;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import com.solace.samples.spring.boot.config.MetricsRegistry;
import com.solace.samples.spring.boot.config.SolaceBinderConfigProperties;
import com.solace.samples.spring.common.SensorReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@Slf4j
public class SolacePublisher {

    @Autowired
    private SolaceBinderConfigProperties configProperties;
    private DirectMessagePublisher publisher;
    private OutboundMessageBuilder messageBuilder;

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {

        //1. Set up the properties including username, password, vpnHostUrl and other control parameters.
        final Properties properties = setupPropertiesForConnection();

        final MessagingService messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();  // blocking connect
        setupConnectivityHandlingInMessagingService(messagingService);

        publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1)
                .build();
        publisher.start();

        // can be called for ACL violations,
        publisher.setPublishFailureListener(e -> {
            System.out.println("### FAILED PUBLISH " + e);
        });
        messageBuilder = messagingService.messageBuilder();
    }

    public void publishMessage(final SensorReading sensorReading) {
        try {
            final OutboundMessage message = messageBuilder.build(sensorReading.toString());  // binary payload message
            publisher.publish(message, Topic.of((configProperties.getTopicName() + sensorReading.getSensorID())));
            MetricsRegistry.successCounter.increment();
        } catch (final RuntimeException runtimeException) {
            log.error("Error encountered while publishing event, exception :{}", runtimeException);
            MetricsRegistry.errorCounter.increment();
        }
    }

    private static void setupConnectivityHandlingInMessagingService(final MessagingService messagingService) {
        messagingService.addServiceInterruptionListener(serviceEvent -> System.out.println("### SERVICE INTERRUPTION: " + serviceEvent.getCause()));
        messagingService.addReconnectionAttemptListener(serviceEvent -> System.out.println("### RECONNECTING ATTEMPT: " + serviceEvent));
        messagingService.addReconnectionListener(serviceEvent -> System.out.println("### RECONNECTED: " + serviceEvent));
    }

    private Properties setupPropertiesForConnection() {
        final Properties properties = new Properties();
        properties.setProperty(SolaceProperties.TransportLayerProperties.HOST, configProperties.getHostUrl());          // host:port
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, configProperties.getVpnName());     // message-vpn
        properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, configProperties.getUserName());      // client-username
        properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, configProperties.getPassword());  // client-password
        properties.setProperty(SolaceProperties.TransportLayerProperties.RECONNECTION_ATTEMPTS, configProperties.getReconnectionAttempts());  // recommended settings
        properties.setProperty(SolaceProperties.TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, configProperties.getConnectionRetriesPerHost());
        return properties;
    }
}
