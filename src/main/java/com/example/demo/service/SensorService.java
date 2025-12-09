package com.example.demo.service;

import com.example.demo.dto.SensorDataDTO;
import com.example.demo.model.SensorData;
import com.example.demo.model.Bus;
import com.example.demo.repository.SensorDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SensorService {
    
    private final SensorDataRepository sensorDataRepository;
    private final BusService busService;
    
    public SensorService(SensorDataRepository sensorDataRepository, BusService busService) {
        this.sensorDataRepository = sensorDataRepository;
        this.busService = busService;
    }
    
    public SensorData processSensorData(SensorDataDTO sensorDataDTO) {
        // Находим автобус
        Bus bus = busService.getById(sensorDataDTO.getBusId());
        if (bus == null) {
            throw new IllegalArgumentException("Автобус с ID " + sensorDataDTO.getBusId() + " не найден");
        }
        
        // Создаем и сохраняем данные датчика
        SensorData sensorData = new SensorData();
        sensorData.setBus(bus);
        sensorData.setSensorType(sensorDataDTO.getSensorType());
        sensorData.setValue(sensorDataDTO.getValue());
        sensorData.setTimestamp(LocalDateTime.now());
        sensorData.setAnomaly(detectAnomaly(sensorDataDTO));
        
        return sensorDataRepository.save(sensorData);
    }
    
    public List<SensorData> getSensorHistory(Long busId) {
        return sensorDataRepository.findByBusId(busId);
    }
    
    public List<SensorData> getAnomalies() {
        return sensorDataRepository.findByAnomalyTrue();
    }
    
    public List<SensorData> getLatestReadings(Long busId) {
        return sensorDataRepository.findLatestByBusId(busId);
    }
    
    private boolean detectAnomaly(SensorDataDTO data) {
        if (data.getSensorType() == null || data.getValue() == null) {
            return false;
        }
        
        switch(data.getSensorType()) {
            case ENGINE_TEMP:
                return data.getValue() > 95.0;
            case TIRE_PRESSURE:
                return data.getValue() < 2.2 || data.getValue() > 2.8;
            case FUEL_LEVEL:
                return data.getValue() < 15.0;
            default:
                return false;
        }
    }
    
    @Transactional
    public SensorData saveSensorData(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }
    
    @Transactional
    public List<SensorData> saveAllSensorData(List<SensorData> sensorDataList) {
        return sensorDataRepository.saveAll(sensorDataList);
    }
}