package com.example.demo.controller;

import com.example.demo.model.Bus;
import com.example.demo.service.BusService;
import com.example.demo.exception.AppException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
public class BusController {
    
    @Autowired
    private BusService busService;

    @GetMapping
    public ResponseEntity<List<Bus>> getAllBuses() {
        List<Bus> buses = busService.getAllBuses();
        return ResponseEntity.ok(buses);
    }

    @PostMapping
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus) {
        Bus createdBus = busService.createBus(bus);
        return ResponseEntity.ok(createdBus);
    }

    @GetMapping("{id}")
    public ResponseEntity<Bus> getBusById(@PathVariable Long id) {
        Bus bus = busService.getBusById(id)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found"));
        return ResponseEntity.ok(bus);
    }

    @PutMapping("{id}")
    public ResponseEntity<Bus> updateBus(@PathVariable Long id, @RequestBody Bus bus) {
        Bus updatedBus = busService.updateBus(id, bus);
        if (updatedBus != null) {
            return ResponseEntity.ok(updatedBus);
        } else {
            throw new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found");
        }
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        boolean deleted = busService.deleteBus(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            throw new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found");
        }
    }
}
