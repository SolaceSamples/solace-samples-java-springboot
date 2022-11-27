package com.solace.samples.spring.boot.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsRegistry {

    @Bean
    static SimpleMeterRegistry threadMetrics() {
        return new SimpleMeterRegistry();
    }

    public static Counter successCounter = Counter
            .builder("solace_samples_message_counter_success")
            .description("Counts the number of successfully produced events")
            .tags("dev", "performance")
            .register(threadMetrics());

    public static Counter errorCounter = Counter
            .builder("solace_samples_message_counter_error")
            .description("Counts the number of failures in event creation")
            .tags("dev", "performance")
            .register(threadMetrics());

}
