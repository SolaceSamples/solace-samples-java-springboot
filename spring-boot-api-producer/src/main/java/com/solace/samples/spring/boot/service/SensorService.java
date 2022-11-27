package com.solace.samples.spring.boot.service;

import com.solace.samples.spring.common.SensorReading;

public interface SensorService {

    public void publishSensorReading(final SensorReading sensorReading);
}
