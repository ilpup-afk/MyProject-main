package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.exception.AppException;
import com.example.demo.model.Bus;
import com.example.demo.service.BusService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "bus-controller",
    description = """
        Контроллер для управления автобусами (CRUD).
        Позволяет создавать, читать, обновлять и удалять автобусы.
        """
)
@RestController
@RequestMapping("/api/buses")
public class BusController {

    @Autowired
    private BusService busService;

    @Operation(
        summary = "Получить список всех автобусов",
        description = """
            Возвращает список всех автобусов в базе данных.

            ### Когда использовать:
            - Для просмотра созданных автобусов
            - Для получения busId перед созданием SensorData

            ### Примечание:
            - Параметры не требуются.
            """,
        tags = {"bus-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список автобусов успешно получен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping
    public ResponseEntity<List<Bus>> getAllBuses() {
        List<Bus> buses = busService.getAllBuses();
        return ResponseEntity.ok(buses);
    }

    @Operation(
        summary = "Создать новый автобус",
        description = """
            Создаёт автобус в базе данных.

            ### Требования:
            - Рекомендуется передавать только поле model.
            - Поле id НЕ передавать (генерируется БД автоматически).

            ### Возможные ошибки:
            - При уникальном ограничении на model попытка создать дубликат
              приведёт к ошибке БД.
            """,
        tags = {"bus-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Автобус успешно создан"),
        @ApiResponse(responseCode = "400", description = "Некорректное тело запроса"),
        @ApiResponse(responseCode = "409", description = "Конфликт (например, model уже существует)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus) {
        Bus createdBus = busService.createBus(bus);
        return ResponseEntity.ok(createdBus);
    }

    @Operation(
        summary = "Получить автобус по ID",
        description = """
            Возвращает автобус по идентификатору.

            ### Ошибки:
            - 404 если автобус не найден.
            """,
        tags = {"bus-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Автобус найден"),
        @ApiResponse(responseCode = "404", description = "Автобус с указанным id не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @GetMapping("{id}")
    public ResponseEntity<Bus> getBusById(@PathVariable Long id) {
        Bus bus = busService.getBusById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found"));
        return ResponseEntity.ok(bus);
    }

    @Operation(
        summary = "Обновить автобус",
        description = """
            Обновляет автобус по идентификатору.

            ### Процесс:
            1) Ищем автобус по id
            2) Меняем поля (обычно model)
            3) Сохраняем в БД

            ### Ошибки:
            - 404 если автобус не найден.
            """,
        tags = {"bus-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Автобус успешно обновлён"),
        @ApiResponse(responseCode = "400", description = "Некорректное тело запроса"),
        @ApiResponse(responseCode = "404", description = "Автобус с указанным id не найден"),
        @ApiResponse(responseCode = "409", description = "Конфликт (например, запись была изменена/удалена в другой транзакции)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PutMapping("{id}")
    public ResponseEntity<Bus> updateBus(@PathVariable Long id, @RequestBody Bus bus) {
        Bus updatedBus = busService.updateBus(id, bus);
        if (updatedBus != null) {
            return ResponseEntity.ok(updatedBus);
        }
        throw new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found");
    }

    @Operation(
        summary = "Удалить автобус",
        description = """
            Удаляет автобус по идентификатору.

            ### Важно:
            - Если на автобус есть ссылки (например, SensorData с FK на bus_id),
              БД может запретить удаление (будет ошибка ограничения внешнего ключа).

            ### Ошибки:
            - 404 если автобус не найден.
            """,
        tags = {"bus-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Автобус успешно удалён (No Content)"),
        @ApiResponse(responseCode = "404", description = "Автобус с указанным id не найден"),
        @ApiResponse(responseCode = "409", description = "Конфликт (например, нельзя удалить из-за связанных данных)"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        boolean deleted = busService.deleteBus(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        throw new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found");
    }
}
