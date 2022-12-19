package com.solace.samples.spring.boot.service.publisher;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.resources.Topic;
import com.solace.samples.spring.boot.config.SolaceConfigProperties;
import com.solace.samples.spring.common.SensorReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;

@Component
@Slf4j
public class SolacePublisher {

    @Autowired
    private SolaceConfigProperties configProperties;
    private DirectMessagePublisher publisher;
    private OutboundMessageBuilder messageBuilder;
    private MessagingService messagingService;

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {

        //1. Set up the properties including username, password, vpnHostUrl and other control parameters.
        final Properties properties = setupPropertiesForConnection();

        //2. Create the MessagingService object and establishes the connection with the Solace event broker
        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();  // This is a blocking connect action

        //3. Register event handlers and callbacks for connection error handling.
        setupConnectivityHandlingInMessagingService(messagingService);

        //4. Build and start the publisher object
        publisher = messagingService.createDirectMessagePublisherBuilder()
                .onBackPressureWait(1)
                .build();
        // can be called for ACL violations
        publisher.setPublishFailureListener(e -> System.out.println("### FAILED PUBLISH " + e));
        publisher.start();

        //5. Build the messageBuilder instance
        messageBuilder = messagingService.messageBuilder();
    }

    public void publishMessage(final SensorReading sensorReading) {
        try {
            //Build the message according to business requirement
            final OutboundMessage message = messageBuilder.build(sensorReading.toString());  // binary payload message
            //Publish the message with the required topic (if required dynamic) identifier.
            publisher.publish(message, Topic.of((configProperties.getTopicName() + sensorReading.getSensorID())));
            log.info("Published SensorReading event :{} on topic : {}", sensorReading, (configProperties.getTopicName() + sensorReading.getSensorID()));
        } catch (final RuntimeException runtimeException) {
            log.error("Error encountered while publishing event, exception :", runtimeException);
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

    //This method will be called once just before this bean is removed from the application context
    // and can be used to do housekeeping activities like publisher termination and messagingService disconnection
    @PreDestroy
    public void houseKeepingOnBeanDestroy() {
        log.info("The bean is getting destroyed, doing housekeeping activities");
        publisher.terminate(1000);
        messagingService.disconnect();
    }
}
