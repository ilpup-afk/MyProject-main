package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
@Entity
@Table(name = "products")
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank(message = "Название пустое")
    @Size(min = 2, max = 100, message = "Название должно быть от 2 до 100 символов")
    private String model;

    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<SensorData> lastReadings = new ArrayList<>();

    // Конструкторы
    public Bus() {}

    public Bus(String model) {
        this.model = model;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<SensorData> getLastReadings() { return lastReadings; }
    public void setLastReadings(List<SensorData> lastReadings) { this.lastReadings = lastReadings; }

    // Вспомогательные методы для управления связью
    public void addSensorData(SensorData data) {
        lastReadings.add(data);
        data.setBus(this);
    }

    public void removeSensorData(SensorData data) {
        lastReadings.remove(data);
        data.setBus(null);
    }
}