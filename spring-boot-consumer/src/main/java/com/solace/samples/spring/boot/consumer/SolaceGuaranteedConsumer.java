package com.solace.samples.spring.boot.consumer;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.MessageAcknowledgementConfiguration;
import com.solace.messaging.config.MessageAcknowledgementConfiguration.Outcome;
import com.solace.messaging.config.MissingResourcesCreationConfiguration;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.receiver.DirectMessageReceiver;
import com.solace.messaging.receiver.MessageReceiver;
import com.solace.messaging.receiver.PersistentMessageReceiver;
import com.solace.messaging.resources.Queue;
import com.solace.messaging.resources.TopicSubscription;
import com.solace.samples.spring.boot.config.SolaceConfigProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;

@Component
@Slf4j
public class SolaceGuaranteedConsumer {

    @Autowired
    private SolaceConfigProperties configProperties;
    private PersistentMessageReceiver persistentMessageReceiver;
    private MessagingService messagingService;

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) throws InterruptedException {

        //1. Set up the properties including username, password, vpnHostUrl and other control parameters.
        final Properties properties = setupPropertiesForConnection();

        //2. Create the MessagingService object and establishes the connection with the Solace event broker
        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();  // This is a blocking connect action
        setupConnectivityHandlingInMessagingService(messagingService);

        //3. Build and start the receiver object
        persistentMessageReceiver = messagingService.createPersistentMessageReceiverBuilder()
                .withRequiredMessageClientOutcomeOperationSupport(
                        new MessageAcknowledgementConfiguration.Outcome[]{Outcome.ACCEPTED, Outcome.FAILED, Outcome.REJECTED})
                .withMissingResourcesCreationStrategy(                      // Configures the missing resources creation strategy.
                        MissingResourcesCreationConfiguration.MissingResourcesCreationStrategy.CREATE_ON_START)   // The strategy to attempt create missing resources when the connection is established.
                //.withMessageAutoAcknowledgement()                         // Client message ack is default behavior.  Enable for auto ack

                .build(Queue.durableExclusiveQueue(configProperties.getQueueName()))           // If it does not exist, this configuration will provision the non-durable queue on the broker when the start() method is called.
                ;

        persistentMessageReceiver.setReceiveFailureListener(failedReceiveEvent -> System.out.println("### FAILED RECEIVE EVENT " + failedReceiveEvent));
        persistentMessageReceiver.start();

        //4. Add topic subscriptions to the queue
        for (String topic : configProperties.getTopicSubscriptions()) {
            persistentMessageReceiver.addSubscription(TopicSubscription.of(topic));
        }

        //5. Receive events in an async/non-blocking manner
        persistentMessageReceiver.receiveAsync((inboundMessage) -> {
                    try {
                        // Do something w/ the message
                        final String payload = inboundMessage.getPayloadAsString();
                        log.info("Processing incoming payload :{} on topic :{}", payload, inboundMessage.getDestinationName());

                        // When processing is complete, ack the message
                        persistentMessageReceiver.settle(inboundMessage, Outcome.ACCEPTED);

                        // example if the message was bad (either format or failed to process)
                        // send REJECT back to broker.  The broker will discard the message to DMQ if
                        // configured
                        //persistentMessageReceiver.settle(inboundMessage, Outcome.REJECTED);
                    }
                    catch (Exception ex) {
                        // Error in processing of message, send NACK back to broker to attempt to send again
                        log.error("Error in processing inbound message ", ex);
                        persistentMessageReceiver.settle(inboundMessage, Outcome.FAILED);
                    }

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
    public void houseKeepingOnBeanDestroy() throws InterruptedException {
        log.info("The bean is getting destroyed, doing housekeeping activities");
        persistentMessageReceiver.terminate(1000);
        messagingService.disconnect();
    }
}
