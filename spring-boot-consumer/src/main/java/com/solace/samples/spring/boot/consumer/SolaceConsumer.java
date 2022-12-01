package com.solace.samples.spring.boot.consumer;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver;
import com.solace.messaging.resources.TopicSubscription;
import com.solace.samples.spring.boot.config.SolaceConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;

@Component
@Slf4j
public class SolaceConsumer {

    @Autowired
    private SolaceConfigProperties configProperties;
    private DirectMessageReceiver directMessageReceiver;
    private MessagingService messagingService;

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {

        //1. Set up the properties including username, password, vpnHostUrl and other control parameters.
        final Properties properties = setupPropertiesForConnection();

        //2. Create the MessagingService object and establishes the connection with the Solace event broker
        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();  // This is a blocking connect action
        setupConnectivityHandlingInMessagingService(messagingService);

        //3. Build and start the receiver object
        directMessageReceiver = messagingService.createDirectMessageReceiverBuilder()
                .withSubscriptions(TopicSubscription.of(configProperties.getTopicName()))
                .build();
        directMessageReceiver.setReceiveFailureListener(failedReceiveEvent -> System.out.println("### FAILED RECEIVE EVENT " + failedReceiveEvent));
        directMessageReceiver.start();

        //4. Build the handler that will be executed for each incoming event
        final MessageReceiver.MessageHandler messageHandler = buildMessageHandler();
        //5. Receive events in an async/non-blocking manner
        directMessageReceiver.receiveAsync(messageHandler);
    }

    private MessageReceiver.MessageHandler buildMessageHandler() {
        return (
                inboundMessage -> {
                    final String payload = inboundMessage.getPayloadAsString();
                    log.info("Processing incoming payload :{} on topic :{}", payload, inboundMessage.getDestinationName());
                }
        );
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
        directMessageReceiver.terminate(1000);
        messagingService.disconnect();
    }
}
