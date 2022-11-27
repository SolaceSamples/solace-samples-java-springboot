package com.solace.samples.spring.boot;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootApiProducerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootApiProducerApplication.class, args);
	}
}
