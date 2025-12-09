package com.example.demo.controller;

import java.io.IOException;
import com.example.demo.service.SensorService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.service.FileService;

@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final SensorService sensorService;

    @PostMapping(value = "/upload/{id}",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            String resultFile = fileService.storeFile(file);
            sensorService.addFileToSensorData(id, resultFile);
                return ResponseEntity.ok(resultFile);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}