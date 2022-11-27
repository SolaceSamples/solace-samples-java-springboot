package com.solace.samples.spring.common;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
@NoArgsConstructor
@ToString
public class SensorReading implements Serializable {
    private String timestamp;
    private String sensorID;
    private Double temperature;
    private BaseUnit baseUnit;

    public enum BaseUnit {
        CELSIUS,
        FAHRENHEIT
    }

}
