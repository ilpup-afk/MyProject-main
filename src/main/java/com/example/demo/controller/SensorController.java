package com.example.demo.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CsvImportResult;
import com.example.demo.dto.SensorDataCreateDTO;
import com.example.demo.model.Bus;
import com.example.demo.model.SensorData;
import com.example.demo.service.SensorService;
import com.example.demo.service.BusService;
import com.example.demo.service.CsvImportService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Slf4j
@RestController
@RequestMapping("/api/sensors")
public class SensorController {
    private final SensorService sensorService;
    private final BusService busService; 
    private final CsvImportService csvImportService;
    public SensorController(SensorService sensorService, BusService busService, CsvImportService csvImportService) {
        this.sensorService = sensorService;
        this.busService = busService;
        this.csvImportService = csvImportService;
    }
    

    @PostMapping
    public ResponseEntity<SensorData> createSensorData(@RequestBody SensorDataCreateDTO dto) {
        Bus bus = busService.getBusById(dto.getBusId())
                .orElseThrow(() -> new RuntimeException("Bus not found"));
        SensorData sensorData = new SensorData();
        sensorData.setSensorType(dto.getSensorType());
        sensorData.setValue(dto.getValue());
        sensorData.setTimestamp(dto.getTimestamp());
        sensorData.setAnomaly(dto.isAnomaly());
        sensorData.setBus(bus);
        SensorData saved = sensorService.createSensorData(sensorData);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public List<SensorData> getAllSensorData(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type) {
        return sensorService.getAll();
    }

    @GetMapping("{busId}")
    public List<SensorData> getSensorDataByBusId(@PathVariable Long busId) {
        return sensorService.getSensorDataByBusId(busId);
    }

    @PutMapping("{id}")
    public ResponseEntity<SensorData> updateSensorData(@PathVariable Long id, @RequestBody @Valid SensorData updatedSensorData) {
        SensorData sensorData = sensorService.updateSensorData(id, updatedSensorData);
        if (sensorData != null) {
            return ResponseEntity.ok(sensorData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteSensorData(@PathVariable Long id) {
        boolean deleted = sensorService.deleteSensorData(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/import-csv", consumes = "multipart/form-data")
    @Operation(
        summary = "Import sensor data from CSV file",
        description = "Upload a CSV file to import sensor data records into the database"
    )
    @ApiResponse(
        responseCode = "200",
        description = "CSV file imported successfully"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid file format or empty file"
    )
    @ApiResponse(
        responseCode = "422",
        description = "CSV file imported with errors in some rows"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error"
    )
    public ResponseEntity<CsvImportResult> importCsv(
            @Parameter(description = "CSV file to upload", required = true)
            @RequestParam("file")
            MultipartFile file) {
        log.info("Received CSV file import request: {} ({} bytes)", 
                file.getOriginalFilename(), file.getSize());
        
        // Check for CSV format
        if (!isCsvFile(file)) {
            CsvImportResult result = new CsvImportResult(0, 1, 
                    List.of("File must be in CSV format"));
            return ResponseEntity.badRequest().body(result);
        }
        
        // Check for empty file
        if (file.isEmpty()) {
            CsvImportResult result = new CsvImportResult(0, 1, 
                    List.of("File is empty"));
            return ResponseEntity.badRequest().body(result);
        }
        
        try {
            CsvImportResult importResult = csvImportService.importProductsFromCsv(file);
            
            if (importResult.hasError()) {
                log.warn("CSV import completed with {} successes and {} failures", 
                        importResult.getSuccessCount(), importResult.getFailedCount());
                return ResponseEntity.unprocessableEntity().body(importResult);
            } else {
                log.info("CSV import successfully completed: {} records imported", 
                        importResult.getSuccessCount());
                return ResponseEntity.ok(importResult);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during CSV import", e);
            CsvImportResult result = new CsvImportResult(0, 1, 
                    List.of("Internal server error: " + e.getMessage()));
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    private boolean isCsvFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }
        
        String contentType = file.getContentType();
        return originalFilename.toLowerCase().endsWith(".csv") ||
               "text/csv".equals(contentType) ||
               "application/vnd.ms-excel".equals(contentType);
    }

}