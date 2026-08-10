package com.solace.samples.spring.boot.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.solace.samples.spring.boot.requester.SolaceRequester;
import com.solace.samples.spring.common.SensorReadingRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/solace/samples/spring/boot/request-reply")
@Slf4j
public class SensorReadingRequestController {

    @Autowired
    private SolaceRequester requester;

    /**
     * POST /solace/samples/spring/boot/request-reply/request
     * Fire-and-observe: publishes the request over guaranteed messaging and returns
     * a correlationId. The reply is delivered asynchronously and logged.
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> submitRequest(@RequestBody final SensorReadingRequest request)
            throws JsonProcessingException {
        log.info("Received HTTP POST /request body={}", request);
        final String correlationId = requester.sendRequest(request);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of("correlationId", correlationId));
    }
}
