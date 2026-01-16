package com.example.demo.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
import com.example.demo.exception.AppException;
import com.example.demo.model.Bus;
import com.example.demo.model.SensorData;
import com.example.demo.service.BusService;
import com.example.demo.service.CsvImportService;
import com.example.demo.service.SensorService;
import com.example.demo.service.TelegramLogService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@Tag(
    name = "sensor-controller",
    description = """
        Контроллер для управления показаниями датчиков (SensorData).
        """
)
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private final SensorService sensorService;
    private final BusService busService;
    private final CsvImportService csvImportService;
    private final TelegramLogService telegramLogService;

    public SensorController(
            SensorService sensorService,
            BusService busService,
            CsvImportService csvImportService,
            TelegramLogService telegramLogService) {

        this.sensorService = sensorService;
        this.busService = busService;
        this.csvImportService = csvImportService;
        this.telegramLogService = telegramLogService;
    }

    @Operation(
        summary = "Создать показание датчика",
        description = """
            Создаёт новую запись SensorData для конкретного автобуса.

            ### Процесс:
            1) Проверяется существование автобуса по busId
            2) Создаётся SensorData и заполняются поля из DTO
            3) Сохраняется в БД

            ### Требования:
            - busId должен существовать в таблице автобусов
            - sensorType должен быть валидным значением enum (например ENGINE_TEMP / TIRE_PRESSURE / FUEL_LEVEL)

            ### Ошибки:
            - 404 если автобус не найден
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Показание датчика успешно создано"),
        @ApiResponse(responseCode = "400", description = "Некорректное тело запроса"),
        @ApiResponse(responseCode = "404", description = "Автобус не найден (Bus with id ... not found)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping
    public ResponseEntity<SensorData> createSensorData(@RequestBody SensorDataCreateDTO dto) {
        Bus bus = busService.getBusById(dto.getBusId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Bus with id " + dto.getBusId() + " not found"));

        SensorData sensorData = new SensorData();
        sensorData.setSensorType(dto.getSensorType());
        sensorData.setValue(dto.getValue());
        sensorData.setTimestamp(dto.getTimestamp());
        sensorData.setAnomaly(dto.isAnomaly());
        sensorData.setBus(bus);

        SensorData saved = sensorService.createSensorData(sensorData);
        return ResponseEntity.ok(saved);
    }

    @Operation(
        summary = "Получить все показания датчиков",
        description = """
            Возвращает список всех записей SensorData.

            ### Параметры:
            - pageable (page/size) — в текущей реализации не используется внутри сервиса (возвращается полный список)
            - type (опционально) — в текущей реализации не используется, добавлен под будущее

            ### Примечания:
            - Если нужно реальное пагинирование/фильтрация по type, надо доработать SensorService/Repository.
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список показаний успешно получен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public List<SensorData> getAllSensorData(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String type) {

        return sensorService.getAll();
    }

    @Operation(
        summary = "Получить показания по busId",
        description = """
            Возвращает список показаний SensorData для конкретного автобуса.

            ### Процесс:
            1) Проверяется существование автобуса по busId
            2) Возвращаются все SensorData, привязанные к busId

            ### Ошибки:
            - 404 если автобус не найден
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Показания для автобуса успешно получены"),
        @ApiResponse(responseCode = "404", description = "Автобус не найден (Bus with id ... not found)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("{busId}")
    public List<SensorData> getSensorDataByBusId(@PathVariable Long busId) {
        busService.getBusById(busId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Bus with id " + busId + " not found"));

        return sensorService.getSensorDataByBusId(busId);
    }

    @Operation(
        summary = "Обновить показание датчика",
        description = """
            Обновляет SensorData по id.

            ### Процесс:
            1) Ищется SensorData по id
            2) Обновляются поля (bus, sensorType, value, timestamp, anomaly)
            3) Сохраняется в БД

            ### Ошибки:
            - 404 если запись SensorData не найдена (зависит от реализации SensorService)
            - 409 при конфликте обновления (если настроен locking/версионирование)
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Показание датчика успешно обновлено"),
        @ApiResponse(responseCode = "400", description = "Некорректное тело запроса"),
        @ApiResponse(responseCode = "404", description = "Показание не найдено"),
        @ApiResponse(responseCode = "409", description = "Конфликт обновления (modified/deleted by another transaction)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PutMapping("{id}")
    public ResponseEntity<SensorData> updateSensorData(
            @PathVariable Long id,
            @RequestBody @Valid SensorData updatedSensorData) {

        SensorData sensorData = sensorService.updateSensorData(id, updatedSensorData);
        return ResponseEntity.ok(sensorData);
    }

    @Operation(
        summary = "Удалить показание датчика",
        description = """
            Удаляет SensorData по id.

            ### Ошибки:
            - 404 если запись не найдена (зависит от реализации SensorService)
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Показание датчика удалено (No Content)"),
        @ApiResponse(responseCode = "404", description = "Показание не найдено"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteSensorData(@PathVariable Long id) {
        sensorService.deleteSensorData(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Импорт SensorData из CSV",
        description = """
            Загружает CSV файл и импортирует записи SensorData в базу данных.

            ### Формат CSV:
            Обязательные поля (заголовки):
            - busId
            - sensorType
            - value
            - timestamp
            - anomaly

            ### Поведение:
            - Если файл не CSV → 400
            - Если файл пустой → 400
            - Если часть строк некорректна → 422 + список ошибок
            - Если всё успешно → 200

            ### Telegram:
            - Отправляет сообщение о результате импорта (успех/частичный успех/ошибка)
            """,
        tags = {"sensor-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "CSV файл импортирован успешно"),
        @ApiResponse(responseCode = "400", description = "Неверный формат или пустой файл"),
        @ApiResponse(responseCode = "422", description = "Импорт выполнен частично: есть ошибки в строках"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping(value = "/import-csv", consumes = "multipart/form-data")
    public ResponseEntity<CsvImportResult> importCsv(
            @Parameter(description = "CSV file to upload", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("Received CSV file import request: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        if (!isCsvFile(file)) {
            CsvImportResult result = new CsvImportResult(0, 1, List.of("File must be in CSV format"));
            telegramLogService.send("CSV IMPORT FAILED: Invalid file format");
            return ResponseEntity.badRequest().body(result);
        }

        if (file.isEmpty()) {
            CsvImportResult result = new CsvImportResult(0, 1, List.of("File is empty"));
            telegramLogService.send("CSV IMPORT FAILED: Empty file");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            CsvImportResult importResult = csvImportService.importProductsFromCsv(file);

            if (importResult.hasError()) {
                log.warn("CSV import completed with {} successes and {} failures",
                        importResult.getSuccessCount(), importResult.getFailedCount());

                telegramLogService.send("CSV IMPORT PARTIAL: " + importResult.getSuccessCount() +
                        " success, " + importResult.getFailedCount() + " failed");

                return ResponseEntity.unprocessableEntity().body(importResult);
            }

            log.info("CSV import successfully completed: {} records imported", importResult.getSuccessCount());
            telegramLogService.send("CSV IMPORT COMPLETE: " + importResult.getSuccessCount() + " records imported");
            return ResponseEntity.ok(importResult);

        } catch (Exception e) {
            log.error("Unexpected error during CSV import", e);

            CsvImportResult result = new CsvImportResult(0, 1,
                    List.of("Internal server error: " + e.getMessage()));

            telegramLogService.send("CSV IMPORT ERROR: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    private boolean isCsvFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String contentType = file.getContentType();
        return originalFilename.toLowerCase().endsWith(".csv")
                || "text/csv".equals(contentType)
                || "application/vnd.ms-excel".equals(contentType);
    }
}
