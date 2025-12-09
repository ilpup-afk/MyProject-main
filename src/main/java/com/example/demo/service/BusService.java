package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import com.example.demo.model.Bus;
import com.example.demo.repository.BusRepository;

@Service
public class BusService {
    private final BusRepository busRepository;
    public BusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Optional<Bus> getBusById(Long id) {
        return busRepository.findById(id);
    }

    public Bus createBus(Bus bus) {
        return busRepository.save(bus);
    }

    public Bus updateBus(Long id, Bus updatedBus) {
        return busRepository.findById(id)
                .map(bus -> {
                    bus.setModel(updatedBus.getModel());
                    return busRepository.save(bus);
                })
                .orElse(null);
    }
    
    public boolean deleteBus(Long id) {
        if (busRepository.existsById(id)) {
            busRepository.deleteById(id);
            return true;
        }
        return false;
    }

}