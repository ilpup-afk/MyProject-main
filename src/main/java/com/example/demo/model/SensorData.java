package com.example.demo.model;

import java.time.LocalDateTime;

import com.example.demo.enums.SensorType;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    @JsonBackReference
    private Bus bus;
    
    @Enumerated(EnumType.STRING)
    private SensorType sensorType;
    
    private Double value;
    
    private LocalDateTime timestamp;
    
    private boolean anomaly;

}
