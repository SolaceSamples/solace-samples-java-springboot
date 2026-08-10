package com.solace.samples.spring.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingRequest implements Serializable {
    private String sensorID;
    private String operation;
}
