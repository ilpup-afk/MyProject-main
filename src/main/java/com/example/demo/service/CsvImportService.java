package com.example.demo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.CsvImportResult;
import com.example.demo.model.Bus;
import com.example.demo.model.SensorData;
import com.example.demo.model.SensorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CsvImportService {
    private final SensorService sensorService;
    private final BusService busService;

    private CSVFormat createCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();
    }

    public CsvImportResult importProductsFromCsv(MultipartFile file) {
        List<SensorData> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser csvParser = new CSVParser(reader, createCsvFormat())) {
            for (CSVRecord csvRecord : csvParser) {
                processCsv(csvRecord, valid, errors);
            }
        } catch (Exception e) {
            String message = "Failed to read CSV file: " + e.getMessage();
            errors.add(message);
            log.error(message, e);
        }

        if (!valid.isEmpty()) {
            try {
                sensorService.saveAllSensorData(valid);
            } catch (Exception e) {
                String msg = "Failed to save sensor data: " + e.getMessage();
                errors.add(msg);
                log.error(msg, e);
            }
        }
        return new CsvImportResult(valid.size(), errors.size(), errors);
    }

    private void processCsv(CSVRecord csvRecord, List<SensorData> valid, List<String> errors) {
        try {
            SensorData sensorData = new SensorData();

            // busId
            String busIdStr = csvRecord.get("busId");
            if (busIdStr == null || busIdStr.isBlank()) {
                throw new IllegalArgumentException("busId is missing");
            }
            Long busId = Long.parseLong(busIdStr);
            Bus bus = busService.getBusById(busId)
                    .orElseThrow(() -> new IllegalArgumentException("Bus not found: " + busId));
            sensorData.setBus(bus);

            // timestamp
            String timestampStr = csvRecord.get("timestamp");
            if (timestampStr == null || timestampStr.isBlank()) {
                throw new IllegalArgumentException("timestamp is missing");
            }
            sensorData.setTimestamp(LocalDateTime.parse(timestampStr));

            // sensortype
            String sensorTypeStr = csvRecord.get("sensorType");
            if (sensorTypeStr == null || sensorTypeStr.isBlank()) {
                throw new IllegalArgumentException("sensorType is missing");
            }
            SensorType sensorType = SensorType.valueOf(sensorTypeStr.trim().toUpperCase());
            sensorData.setSensorType(sensorType);

            // value
            String valueStr = csvRecord.get("value");
            if (valueStr == null || valueStr.isBlank()) {
                throw new IllegalArgumentException("value is missing");
            }
            sensorData.setValue(Double.parseDouble(valueStr));

            // anomaly
            String anomalyStr = csvRecord.isMapped("anomaly") ? csvRecord.get("anomaly") : null;
            if (anomalyStr != null && !anomalyStr.isBlank()) {
                sensorData.setAnomaly(Boolean.parseBoolean(anomalyStr));
            } else {
                // check for anomaly
                sensorData.setAnomaly(sensorService.checkForAnomaly(sensorData));
            }

            valid.add(sensorData);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            String msg = String.format("Row %d: invalid data - %s", csvRecord.getRecordNumber(), e.getMessage());
            errors.add(msg);
            log.warn(msg);
        } catch (Exception e) {
            String msg = String.format("Row %d: unexpected error - %s", csvRecord.getRecordNumber(), e.getMessage());
            errors.add(msg);
            log.error(msg, e);
        }
    }
}