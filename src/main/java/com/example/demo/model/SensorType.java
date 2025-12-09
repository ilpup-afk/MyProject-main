package com.example.demo.model;

public enum SensorType {
    ENGINE_TEMP("engine_temp"),
    TIRE_PRESSURE("tire_pressure"),
    FUEL_LEVEL("fuel_level");

    private final String value;

    SensorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}