package com.example.demo.controller;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Bus;
import com.example.demo.service.BusService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MainController {
    private final BusService busService;

    public MainController(BusService busService) {
        this.busService = busService;
    }

    @GetMapping("/buses")
    public List<Bus> getBuses(@RequestParam(required = false) String model) {
        return busService.getAll();
    }

    @GetMapping("/buses/{id}")
    public ResponseEntity<Bus> getBusById(@PathVariable Long id) {
        Bus bus = busService.getById(id);
        if (bus != null) {
            return ResponseEntity.ok(bus);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/buses")
    public ResponseEntity<Bus> addBus(@RequestBody @Valid Bus bus) {
        // Убедимся, что ID null
        bus.setId(null);
        Bus createdBus = busService.create(bus);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBus);
    }

    @PutMapping("/buses/{id}")
    public ResponseEntity<Bus> updateBus(@PathVariable Long id, @RequestBody @Valid Bus updatedBus) {
        Bus bus = busService.updateById(id, updatedBus);
        if (bus != null) {
            return ResponseEntity.ok(bus);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/buses/{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        if (busService.deleteById(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Простой endpoint для тестирования
    @GetMapping("/test")
    public String test() {
        return "API работает!";
    }
}