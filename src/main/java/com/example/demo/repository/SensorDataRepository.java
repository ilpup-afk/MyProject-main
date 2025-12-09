package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.SensorData;
import com.example.demo.model.SensorType;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    List<SensorData> findByBusId(Long busId);
    
    List<SensorData> findByBusIdAndSensorType(Long busId, SensorType sensorType);
    
    List<SensorData> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<SensorData> findByAnomalyTrue();
    
    @Query("SELECT sd FROM SensorData sd WHERE sd.bus.id = :busId ORDER BY sd.timestamp DESC LIMIT 10")
    List<SensorData> findLatestByBusId(@Param("busId") Long busId);
    
    // Новые методы для статистики
    @Query("SELECT COUNT(sd) FROM SensorData sd WHERE sd.bus.id = :busId AND sd.anomaly = true")
    long countAnomaliesByBusId(@Param("busId") Long busId);
    
    @Query("SELECT sd FROM SensorData sd WHERE sd.bus.id = :busId AND sd.sensorType = :sensorType ORDER BY sd.timestamp DESC")
    List<SensorData> findLatestByBusIdAndType(@Param("busId") Long busId, @Param("sensorType") SensorType sensorType);
}