package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.List;

import com.example.demo.model.SensorData;
import com.example.demo.model.SensorType;
import com.example.demo.repository.SensorDataRepository;
import com.example.demo.exception.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SensorService {
    
    private final SensorDataRepository sensorDataRepository;
    private final TelegramLogService telegramLogService;

    public List<SensorData> getAll() {
        return sensorDataRepository.findAll();
    }

    public List<SensorData> saveAll(List<SensorData> sensorDataList) {
        List<SensorData> saved = sensorDataRepository.saveAll(sensorDataList);
        telegramLogService.send("📊 SENSORS IMPORTED: " + saved.size() + " records");
        return saved;
    }

    public SensorData getSensorData(Long id) {
        return sensorDataRepository.findById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Sensor data with id " + id + " not found"));
    }

    public SensorData createSensorData(SensorData sensorData) {
        SensorData saved = sensorDataRepository.save(sensorData);
        telegramLogService.send(
            "✅ SENSOR CREATED: id=" + saved.getId() + 
            ", bus=" + saved.getBus().getId() + 
            ", type=" + saved.getSensorType() + 
            ", value=" + saved.getValue()
        );
        return saved;
    }

    public SensorData updateSensorData(Long id, SensorData updatedSensorData) {
        try {
            return sensorDataRepository.findById(id)
                .map(sensorData -> {
                    sensorData.setBus(updatedSensorData.getBus());
                    sensorData.setSensorType(updatedSensorData.getSensorType());
                    sensorData.setValue(updatedSensorData.getValue());
                    sensorData.setTimestamp(updatedSensorData.getTimestamp());
                    sensorData.setAnomaly(updatedSensorData.isAnomaly());
                    SensorData updated = sensorDataRepository.save(sensorData);
                    telegramLogService.send(
                        "✏️ SENSOR UPDATED: id=" + updated.getId() + 
                        ", type=" + updated.getSensorType() + 
                        ", value=" + updated.getValue()
                    );
                    return updated;
                })
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Sensor data with id " + id + " not found"));
        } catch (org.hibernate.StaleObjectStateException e) {
            throw new AppException(HttpStatus.CONFLICT, "Sensor was modified or deleted by another transaction");
        }
    }

    public boolean deleteSensorData(Long id) {
        try {
            if (sensorDataRepository.existsById(id)) {
                sensorDataRepository.deleteById(id);
                telegramLogService.send("🗑️ SENSOR DELETED: id=" + id);
                return true;
            }
            throw new AppException(HttpStatus.NOT_FOUND, "Sensor data with id " + id + " not found");
        } catch (org.hibernate.StaleObjectStateException e) {
            throw new AppException(HttpStatus.CONFLICT, "Sensor was modified or deleted by another transaction");
        }
    }

    public List<SensorData> getSensorDataByBusId(Long busId) {
        return sensorDataRepository.findByBusId(busId);
    }

    public void addFileToSensorData(Long sensorDataId, String filePath) {
        SensorData sensorData = sensorDataRepository.findById(sensorDataId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Sensor data not found"));
        sensorData.setFilePath(filePath);
        sensorDataRepository.save(sensorData);
        telegramLogService.send("📎 FILE ATTACHED: sensor=" + sensorDataId + ", file=" + filePath);
    }

    public boolean checkForAnomaly(SensorData sensorData) {
        SensorType type = sensorData.getSensorType();
        Double value = sensorData.getValue();

        switch (type) {
            case ENGINE_TEMP:
                return value > 100.0 || value < 60.0;
            case TIRE_PRESSURE:
                return value > 3.5 || value < 1.8;
            case FUEL_LEVEL:
                return value > 5.0;
            default:
                return false;
        }
    }
}
