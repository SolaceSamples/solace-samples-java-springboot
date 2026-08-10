package com.solace.samples.spring.boot.replier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.messaging.MessagingService;
import com.solace.messaging.config.MessageAcknowledgementConfiguration.Outcome;
import com.solace.messaging.config.MissingResourcesCreationConfiguration;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.publisher.PersistentMessagePublisher;
import com.solace.messaging.receiver.PersistentMessageReceiver;
import com.solace.messaging.receiver.InboundMessage;
import com.solace.messaging.resources.Queue;
import com.solace.messaging.resources.Topic;
import com.solace.messaging.resources.TopicSubscription;
import com.solace.samples.spring.boot.config.SolaceConfigProperties;
import com.solace.samples.spring.common.SensorReadingRequest;
import com.solace.samples.spring.common.SensorReadingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;

@Component
@Slf4j
public class SolaceReplier {

    @Autowired
    private SolaceConfigProperties config;

    private MessagingService messagingService;
    private PersistentMessagePublisher publisher;
    private PersistentMessageReceiver requestReceiver;
    private OutboundMessageBuilder messageBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) throws InterruptedException {
        // 1. Build connection properties (same shape as sibling modules)
        final Properties properties = setupPropertiesForConnection();

        // 2. Create MessagingService and connect
        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();
        setupConnectivityHandlingInMessagingService(messagingService);

        // 3. Build and start the PersistentMessagePublisher (used to publish replies)
        publisher = messagingService.createPersistentMessagePublisherBuilder().build();
        publisher.start();

        // 4. Build and start the PersistentMessageReceiver on the request queue.
        //    The queue is created on start if it doesn't already exist.
        requestReceiver = messagingService.createPersistentMessageReceiverBuilder()
                .withRequiredMessageClientOutcomeOperationSupport(
                        Outcome.ACCEPTED, Outcome.FAILED, Outcome.REJECTED)
                .withMissingResourcesCreationStrategy(
                        MissingResourcesCreationConfiguration.MissingResourcesCreationStrategy.CREATE_ON_START)
                .build(Queue.durableExclusiveQueue(config.getRequestQueue().getName()));
        requestReceiver.setReceiveFailureListener(fre -> log.error("### FAILED RECEIVE EVENT {}", fre));
        requestReceiver.start();
        requestReceiver.addSubscription(TopicSubscription.of(config.getRequestQueue().getSubscription()));

        // 5. Cache the message builder for outbound replies
        messageBuilder = messagingService.messageBuilder();

        // 6. Start async consumption of requests
        requestReceiver.receiveAsync(this::handleRequest);
    }

    private void handleRequest(final InboundMessage inbound) {
        // Both correlationId and replyTo are carried as user-defined message properties.
        // (See the note in SolaceRequester.sendRequest on why we don't use the SMF
        // correlation-id header in solace-messaging-client 1.6.0.)
        final String correlationId = inbound.getProperty("correlationId");
        final String replyTo = inbound.getProperty("replyTo");
        try {
            final SensorReadingRequest request =
                    mapper.readValue(inbound.getPayloadAsString(), SensorReadingRequest.class);
            log.info("Received request correlationId={} replyTo={} request={}", correlationId, replyTo, request);

            // Validate replyTo — never publish to a null/blank destination.
            // In production, validate against an allow-list (see README).
            if (replyTo == null || replyTo.isBlank()) {
                log.error("Missing replyTo on request correlationId={} — REJECTing to DMQ", correlationId);
                requestReceiver.settle(inbound, Outcome.REJECTED);
                return;
            }

            final SensorReadingResponse response = process(request);
            final String replyPayload = mapper.writeValueAsString(response);

            // Echo correlationId back on the same user property so the requester can match it.
            final OutboundMessage replyMsg = messageBuilder
                    .withProperty("correlationId", correlationId)
                    .build(replyPayload);

            publisher.publish(replyMsg, Topic.of(replyTo));
            log.info("Published reply correlationId={} to topic={} payload={}", correlationId, replyTo, replyPayload);

            requestReceiver.settle(inbound, Outcome.ACCEPTED);
        } catch (Exception ex) {
            // Transient error — FAILED tells the broker to redeliver.
            log.error("Error processing request correlationId={}", correlationId, ex);
            requestReceiver.settle(inbound, Outcome.FAILED);
        }
    }

    private SensorReadingResponse process(final SensorReadingRequest request) {
        // Stub processing logic — in a production replier, swap for real work.
        return new SensorReadingResponse(
                request.getSensorID(),
                request.getOperation(),
                "value-for-" + request.getSensorID() + "-op-" + request.getOperation(),
                "OK",
                null);
    }

    private static void setupConnectivityHandlingInMessagingService(final MessagingService ms) {
        ms.addServiceInterruptionListener(e -> log.warn("### SERVICE INTERRUPTION: {}", e.getCause()));
        ms.addReconnectionAttemptListener(e -> log.info("### RECONNECTING ATTEMPT: {}", e));
        ms.addReconnectionListener(e -> log.info("### RECONNECTED: {}", e));
    }

    private Properties setupPropertiesForConnection() {
        final Properties p = new Properties();
        p.setProperty(SolaceProperties.TransportLayerProperties.HOST, config.getHostUrl());
        p.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, config.getVpnName());
        p.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, config.getUserName());
        p.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, config.getPassword());
        p.setProperty(SolaceProperties.TransportLayerProperties.RECONNECTION_ATTEMPTS, config.getReconnectionAttempts());
        p.setProperty(SolaceProperties.TransportLayerProperties.CONNECTION_RETRIES_PER_HOST, config.getConnectionRetriesPerHost());
        return p;
    }

    @PreDestroy
    public void houseKeepingOnBeanDestroy() {
        log.info("SolaceReplier shutting down — terminating receiver, publisher, disconnecting messagingService");
        if (requestReceiver != null) requestReceiver.terminate(1000);
        if (publisher != null) publisher.terminate(1000);
        if (messagingService != null) messagingService.disconnect();
    }
}
