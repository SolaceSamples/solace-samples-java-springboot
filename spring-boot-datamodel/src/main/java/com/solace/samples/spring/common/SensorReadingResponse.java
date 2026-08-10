package com.solace.samples.spring.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReadingResponse implements Serializable {
    private String sensorID;
    private String operation;
    private String result;
    private String status;         // "OK" | "ERROR"
    private String errorMessage;   // populated when status == ERROR
}
