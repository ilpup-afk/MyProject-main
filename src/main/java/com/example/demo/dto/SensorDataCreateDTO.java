package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.model.SensorType;

import lombok.Data;

@Data
public class SensorDataCreateDTO {
    private SensorType sensorType;
    private Double value;
    private LocalDateTime timestamp;
    private boolean anomaly;
    private Long busId;
}