package com.solace.samples.spring.boot.processor;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver;
import com.solace.messaging.resources.Topic;
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
public class SolaceProcessor {

    @Autowired
    private SolaceConfigProperties configProperties;

    private DirectMessagePublisher directMessagePublisher;
    private DirectMessageReceiver directMessageReceiver;
    private OutboundMessageBuilder messageBuilder;
    private MessagingService messagingService;

    private final String outboundMessageTopicNameSuffix = "/newOutboundMessage";

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
        directMessagePublisher = messagingService.createDirectMessagePublisherBuilder().onBackPressureWait(1).build();
        // can be called for ACL violations
        directMessagePublisher.setPublishFailureListener(e -> System.out.println("### FAILED PUBLISH " + e));
        directMessagePublisher.start();

        //5. Build and start the receiver object
        directMessageReceiver = messagingService.createDirectMessageReceiverBuilder().withSubscriptions(TopicSubscription.of(configProperties.getTopicName())).build();
        directMessageReceiver.setReceiveFailureListener(failedReceiveEvent -> System.out.println("### FAILED RECEIVE EVENT " + failedReceiveEvent));
        directMessageReceiver.start();

        //6. Build the handler that will be executed for each incoming event
        messageBuilder = messagingService.messageBuilder();
        //7. Build the messageBuilder instance
        final MessageReceiver.MessageHandler messageHandler = buildMessageHandler();
        //8. Receive events in an async/non-blocking manner
        directMessageReceiver.receiveAsync(messageHandler);
    }

    private MessageReceiver.MessageHandler buildMessageHandler() {
        return (inboundMessage -> {
            // how to "process" the incoming message? maybe do a DB lookup? add some additional properties? or change the payload?
            final String inboundTopic = inboundMessage.getDestinationName();
            log.info("Processing message on incoming topic :{} with payload:{}", inboundTopic, inboundMessage.getPayloadAsString());
            final String upperCaseMessage = inboundTopic.toUpperCase();  // Simple example of "processing" of an event
            final OutboundMessage outboundMessage = messageBuilder.build(upperCaseMessage);  // build the payload to be set in the outbound event.
            final String outboundTopic = inboundTopic + outboundMessageTopicNameSuffix; // Define the topic name (dynamic if required) for the outbound event
            try {
                log.info("Posting outbound message on topic:{} with payload:{}", outboundTopic, outboundMessage.getPayloadAsString());
                directMessagePublisher.publish(outboundMessage, Topic.of(outboundTopic));
            } catch (RuntimeException e) {  // threw from send(), only thing that is throwing here
                log.error("Runtime exception encountered while publishing message to topic :{}. Error is :{}", outboundTopic, e);
            }
        });
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
        directMessagePublisher.terminate(1000);
        messagingService.disconnect();
    }
}
