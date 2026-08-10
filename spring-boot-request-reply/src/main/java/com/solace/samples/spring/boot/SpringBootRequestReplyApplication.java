package com.solace.samples.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringBootRequestReplyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootRequestReplyApplication.class, args);
    }
}
