package com.example.demo.repository;

import com.example.demo.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SensorDataRepository 
            extends JpaRepository<SensorData, Long> {
            List<SensorData> findByBusId(Long busId);
}