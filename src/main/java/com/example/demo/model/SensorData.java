package com.example.demo.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor_data")
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id")
    @JsonIgnore
    private Bus bus;
    
    @Enumerated(EnumType.STRING)
    private SensorType sensorType;
    
    private Double value;
    
    private LocalDateTime timestamp;
    
    private boolean anomaly;

    // Конструкторы
    public SensorData() {}

    public SensorData(Bus bus, SensorType sensorType, Double value, LocalDateTime timestamp) {
        this.bus = bus;
        this.sensorType = sensorType;
        this.value = value;
        this.timestamp = timestamp;
        this.anomaly = false;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Bus getBus() { return bus; }
    public void setBus(Bus bus) { this.bus = bus; }
    
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public boolean isAnomaly() { return anomaly; }
    public void setAnomaly(boolean anomaly) { this.anomaly = anomaly; }
}