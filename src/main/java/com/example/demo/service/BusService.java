package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Bus;
import com.example.demo.model.SensorData;
import com.example.demo.repository.BusRepository;
import com.example.demo.repository.SensorDataRepository;
import com.example.demo.specifications.BusSpecification;

@Service
@Transactional(readOnly = true)
public class BusService {
    private final BusRepository busRepository;
    private final SensorDataRepository sensorDataRepository;

    public BusService(BusRepository busRepository, SensorDataRepository sensorDataRepository) {
        this.busRepository = busRepository;
        this.sensorDataRepository = sensorDataRepository;
    }

    public List<Bus> getAll() {
        return busRepository.findAll();
    }

    public Bus getById(Long id) {
        return busRepository.findById(id).orElse(null);
    }

    @Transactional
    public Bus create(Bus bus) {
        // Убедимся, что ID null для нового объекта
        bus.setId(null);
        return busRepository.save(bus);
    }

    @Transactional
    public Bus updateById(Long id, Bus updatedBus) {
        return busRepository.findById(id)
                .map(existingBus -> {
                    existingBus.setModel(updatedBus.getModel());
                    return busRepository.save(existingBus);
                })
                .orElse(null);
    }

    @Transactional
    public boolean deleteById(Long id) {
        if (busRepository.existsById(id)) {
            busRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Bus> getByModel(String model) {
        if (model != null && !model.trim().isEmpty()) {
            return busRepository.findByModelContainingIgnoreCase(model);
        }
        return busRepository.findAll();
    }

    public Page<Bus> getByFilter(String model, Pageable pageable) {
        Specification<Bus> spec = BusSpecification.filterByModel(model);
        return busRepository.findAll(spec, pageable);
    }

    // Дополнительные методы для работы с датчиками
    public List<SensorData> getBusSensorData(Long busId) {
        return sensorDataRepository.findByBusId(busId);
    }

    public List<SensorData> getBusAnomalies(Long busId) {
        List<SensorData> allData = sensorDataRepository.findByBusId(busId);
        return allData.stream()
                .filter(SensorData::isAnomaly)
                .toList();
    }

    // Получить последние показания датчиков для автобуса
    public List<SensorData> getLatestSensorReadings(Long busId) {
        return sensorDataRepository.findLatestByBusId(busId);
    }

    public static class BusStatistics {
        private final long totalReadings;
        private final long anomalyCount;

        public BusStatistics(long totalReadings, long anomalyCount) {
            this.totalReadings = totalReadings;
            this.anomalyCount = anomalyCount;
        }

        public long getTotalReadings() { return totalReadings; }
        public long getAnomalyCount() { return anomalyCount; }
        public double getAnomalyPercentage() { 
            return totalReadings > 0 ? (double) anomalyCount / totalReadings * 100 : 0; 
        }
    }
}