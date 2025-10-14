package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.SensorData;
import com.example.demo.repository.SensorDataRepository;

@Service
@Transactional(readOnly = true)
public class SensorDataService {
    
    private final SensorDataRepository sensorDataRepository;
    
    public SensorDataService(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }
    
    public List<SensorData> getByBusId(Long busId) {
        return sensorDataRepository.findByBusId(busId);
    }
    
    public List<SensorData> getLatestByBusId(Long busId) {
        return sensorDataRepository.findLatestByBusId(busId);
    }
    
    public List<SensorData> getAnomalies() {
        return sensorDataRepository.findByAnomalyTrue();
    }
    
    @Transactional
    public SensorData save(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }
    
    @Transactional
    public List<SensorData> saveAll(List<SensorData> sensorDataList) {
        return sensorDataRepository.saveAll(sensorDataList);
    }
}