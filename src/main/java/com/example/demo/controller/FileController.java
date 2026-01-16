package com.example.demo.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.exception.AppException;
import com.example.demo.service.FileService;
import com.example.demo.service.SensorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "file-controller",
    description = """
        Загрузка файлов и привязка файла к записи SensorData.
        """
)
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private final FileService fileService;
    private final SensorService sensorService;

    @Operation(
        summary = "Загрузить файл и привязать к SensorData",
        description = """
            Загружает файл (multipart/form-data) и сохраняет его на сервере, затем записывает путь в SensorData.filePath.

            ### Параметры:
            - id: id записи SensorData
            - file: загружаемый файл

            ### Важно:
            - Ограничение размера: 10 MB (можно поменять в константе + в настройках Spring).
            - Типы файлов определяются FileService (сейчас у тебя разрешён только CSV).
            """,
        tags = {"file-controller"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Файл загружен и привязан к SensorData"),
        @ApiResponse(responseCode = "400", description = "Пустой файл/некорректный запрос/ошибка валидации файла"),
        @ApiResponse(responseCode = "404", description = "SensorData с указанным id не найден"),
        @ApiResponse(responseCode = "413", description = "Файл слишком большой"),
        @ApiResponse(responseCode = "415", description = "Неподдерживаемый тип файла"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    @PostMapping(
        value = "/upload/{id}",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileUploadResponse> uploadFile(
            @PathVariable Long id,
            @Parameter(description = "Файл для загрузки", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large. Max size = " + MAX_FILE_SIZE_BYTES + " bytes");
        }

        try {
            // 1) Сохраняем файл на диск
            String storedPath = fileService.storeFile(file); // сейчас у тебя тут разрешён только CSV [file:52]

            // 2) Привязываем к SensorData
            // addFileToSensorData бросает RuntimeException если SensorData не найдена [file:46]
            try {
                sensorService.addFileToSensorData(id, storedPath);
            } catch (RuntimeException ex) {
                // Если SensorData не найдена — можно удалить файл, чтобы не копить мусор (опционально)
                // fileService.deleteFile(storedPath);
                throw new AppException(HttpStatus.NOT_FOUND, "SensorData with id " + id + " not found");
            }

            FileUploadResponse response = new FileUploadResponse(
                    id,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    storedPath
            );

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Failed to store file: " + ex.getMessage());
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error: " + ex.getMessage());
        }
    }

    public record FileUploadResponse(
            Long sensorDataId,
            String originalFilename,
            String contentType,
            long size,
            String storedPath
    ) {}
}
