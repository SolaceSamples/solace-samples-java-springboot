package com.solace.samples.spring.boot.requester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solace.messaging.MessagingService;
import com.solace.messaging.config.MessageAcknowledgementConfiguration.Outcome;
import com.solace.messaging.config.MissingResourcesCreationConfiguration;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.OutboundMessageBuilder;
import com.solace.messaging.publisher.PersistentMessagePublisher;
import com.solace.messaging.receiver.InboundMessage;
import com.solace.messaging.receiver.PersistentMessageReceiver;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class SolaceRequester {

    @Autowired
    private SolaceConfigProperties config;

    private MessagingService messagingService;
    private PersistentMessagePublisher publisher;
    private PersistentMessageReceiver replyReceiver;
    private OutboundMessageBuilder messageBuilder;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Track in-flight correlationIds for latency logging and timeout sweeping. */
    private final ConcurrentMap<String, Instant> pending = new ConcurrentHashMap<>();

    @EventListener
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) throws InterruptedException {
        // 1. Build connection properties
        final Properties properties = setupPropertiesForConnection();

        // 2. Create MessagingService and connect
        messagingService = MessagingService.builder(ConfigurationProfile.V1).fromProperties(properties).build();
        messagingService.connect();
        setupConnectivityHandlingInMessagingService(messagingService);

        // 3. Build and start the PersistentMessagePublisher (publishes requests to a topic)
        publisher = messagingService.createPersistentMessagePublisherBuilder().build();
        publisher.start();

        // 4. Build and start the PersistentMessageReceiver on the reply queue.
        //    Queue is created on start; it subscribes to the reply topic so the replier
        //    can address the reply by topic name rather than queue name.
        replyReceiver = messagingService.createPersistentMessageReceiverBuilder()
                .withRequiredMessageClientOutcomeOperationSupport(
                        Outcome.ACCEPTED, Outcome.FAILED, Outcome.REJECTED)
                .withMissingResourcesCreationStrategy(
                        MissingResourcesCreationConfiguration.MissingResourcesCreationStrategy.CREATE_ON_START)
                .build(Queue.durableExclusiveQueue(config.getReplyQueue().getName()));
        replyReceiver.setReceiveFailureListener(fre -> log.error("### FAILED RECEIVE EVENT {}", fre));
        replyReceiver.start();
        replyReceiver.addSubscription(TopicSubscription.of(config.getReplyQueue().getSubscription()));

        // 5. Cache the message builder
        messageBuilder = messagingService.messageBuilder();

        // 6. Start async consumption of replies
        replyReceiver.receiveAsync(this::handleReply);
    }

    /**
     * Called by the REST controller. Publishes the request and returns immediately.
     * The reply will be delivered asynchronously and logged.
     */
    public String sendRequest(final SensorReadingRequest request) throws JsonProcessingException {
        final String correlationId = UUID.randomUUID().toString();
        final String payload = mapper.writeValueAsString(request);

        // Both the correlation ID and reply-to topic are carried as user-defined message
        // properties. In solace-messaging-client 1.6.0, setting the SMF correlation-id header
        // via withProperty(SolaceProperties.MessageProperties.CORRELATION_ID, ...) does not
        // round-trip to InboundMessage.getCorrelationId(), so we use plain user properties for
        // both — this is the pattern that reliably works with this SDK version.
        final OutboundMessage message = messageBuilder
                .withProperty("correlationId", correlationId)
                .withProperty("replyTo", config.getReplyQueue().getSubscription())
                .build(payload);

        pending.put(correlationId, Instant.now());
        publisher.publish(message, Topic.of(config.getRequestTopic()));
        log.info("Published request correlationId={} topic={} payload={}",
                correlationId, config.getRequestTopic(), payload);
        return correlationId;
    }

    private void handleReply(final InboundMessage inbound) {
        final String correlationId = inbound.getProperty("correlationId");
        try {
            if (correlationId == null || correlationId.isBlank()) {
                log.warn("Reply arrived without a correlationId user property — dropping to DMQ");
                replyReceiver.settle(inbound, Outcome.REJECTED);
                return;
            }
            final SensorReadingResponse response =
                    mapper.readValue(inbound.getPayloadAsString(), SensorReadingResponse.class);
            final Instant sentAt = pending.remove(correlationId);
            final long latencyMs = sentAt == null ? -1L : Duration.between(sentAt, Instant.now()).toMillis();
            if (sentAt == null) {
                log.warn("Received reply for unknown correlationId={} — possibly late or duplicate", correlationId);
            }
            log.info("Received reply correlationId={} latencyMs={} response={}",
                    correlationId, latencyMs, response);
            replyReceiver.settle(inbound, Outcome.ACCEPTED);
        } catch (Exception ex) {
            log.error("Failed to process reply correlationId={}", correlationId, ex);
            replyReceiver.settle(inbound, Outcome.FAILED);
        }
    }

    /**
     * Sweep the pending map for entries older than the configured timeout.
     * Log a warning and drop them so the map does not grow unbounded when replies never arrive.
     */
    @Scheduled(fixedDelayString = "${solace.replyTimeoutMs}")
    public void sweepPendingTimeouts() {
        final Instant now = Instant.now();
        final long timeoutMs = config.getReplyTimeoutMs();
        final Iterator<Map.Entry<String, Instant>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, Instant> entry = it.next();
            if (Duration.between(entry.getValue(), now).toMillis() > timeoutMs) {
                log.warn("Reply timeout exceeded for correlationId={} (waited > {} ms) — dropping from pending map",
                        entry.getKey(), timeoutMs);
                it.remove();
            }
        }
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
        log.info("SolaceRequester shutting down — terminating reply receiver, publisher, disconnecting messagingService");
        if (replyReceiver != null) replyReceiver.terminate(1000);
        if (publisher != null) publisher.terminate(1000);
        if (messagingService != null) messagingService.disconnect();
    }
}
