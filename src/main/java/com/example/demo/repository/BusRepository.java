package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Bus;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long>, JpaSpecificationExecutor<Bus> {
    
    // Поиск по модели (с игнорированием регистра)
    List<Bus> findByModelContainingIgnoreCase(String model);
    
    // Проверка существования по модели
    boolean existsByModelIgnoreCase(String model);
    
    // Поиск по точному совпадению модели
    Optional<Bus> findByModel(String model);
    
    // Кастомный запрос для поиска
    @Query("SELECT b FROM Bus b WHERE LOWER(b.model) LIKE LOWER(CONCAT('%', :model, '%'))")
    List<Bus> searchByModel(@Param("model") String model);
    
    // Подсчет автобусов с аномалиями
    @Query("SELECT COUNT(DISTINCT b) FROM Bus b JOIN b.sensorData sd WHERE sd.anomaly = true")
    long countBusesWithAnomalies();
    
    // Найти автобусы с аномалиями
    @Query("SELECT DISTINCT b FROM Bus b JOIN b.sensorData sd WHERE sd.anomaly = true")
    List<Bus> findBusesWithAnomalies();
}