package com.solace.samples.spring.boot.api;

import com.solace.samples.spring.boot.service.SensorService;
import com.solace.samples.spring.common.SensorReading;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solace/samples/spring/boot/producer")
public class SensorController {

    @Autowired
    private SensorService sensorService;

    @PostMapping("/sensor/reading")
    @ResponseStatus(HttpStatus.CREATED)
    public void publishSensorReading(@RequestBody SensorReading sensorReading) {
        sensorService.publishSensorReading(sensorReading);
    }

}
