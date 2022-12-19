package com.solace.samples.spring.boot.api;

import com.solace.samples.spring.boot.service.SensorService;
import com.solace.samples.spring.common.SensorReading;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solace/samples/spring/boot/producer")
@Slf4j
public class SensorController {

    @Autowired
    private SensorService sensorService;

    @PostMapping("/sensor/reading")
    @ResponseStatus(HttpStatus.CREATED)
    public void publishSensorReading(@RequestBody SensorReading sensorReading) {
        log.info("Processing incoming POST request for SensorReading :{}", sensorReading);
        sensorService.publishSensorReading(sensorReading);
    }

}
