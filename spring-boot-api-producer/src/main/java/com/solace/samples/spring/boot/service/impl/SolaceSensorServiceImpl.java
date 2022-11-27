package com.solace.samples.spring.boot.service.impl;

import com.solace.samples.spring.boot.service.publisher.SolacePublisher;
import com.solace.samples.spring.boot.service.SensorService;
import com.solace.samples.spring.common.SensorReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SolaceSensorServiceImpl implements SensorService {

    @Autowired
    private SolacePublisher publisher;

    @Override
    public void publishSensorReading(final SensorReading sensorReading) {
        createPayload(sensorReading);
        publisher.publishMessage(sensorReading);
    }

    private void createPayload(final SensorReading sensorReading) {
        //do transformations or create the payload as required by the business logic.
        sensorReading.setTimestamp(Instant.now().toString());
    }


}
