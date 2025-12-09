package com.example.demo.dto;

import com.example.demo.enums.Action;
import com.example.demo.model.SensorType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AlertDTO {
    private Long id;
    private SensorType sensorType;
    private Action action;
}