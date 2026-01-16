package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import com.example.demo.model.Bus;
import com.example.demo.repository.BusRepository;
import com.example.demo.exception.AppException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusService {
    
    private final BusRepository busRepository;
    private final TelegramLogService telegramLogService;

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public Optional<Bus> getBusById(Long id) {
        return busRepository.findById(id);
    }

    public Bus createBus(Bus bus) {
        Bus created = busRepository.save(bus);
        telegramLogService.send("✅ BUS CREATED: id=" + created.getId() + ", model=" + created.getModel());
        return created;
    }

    public Bus updateBus(Long id, Bus updatedBus) {
        try {
            return busRepository.findById(id)
                    .map(bus -> {
                        bus.setModel(updatedBus.getModel());
                        Bus updated = busRepository.save(bus);
                        telegramLogService.send("✏️ BUS UPDATED: id=" + updated.getId() + ", model=" + updated.getModel());
                        return updated;
                    })
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Bus with id " + id + " not found"));
        } catch (org.hibernate.StaleObjectStateException e) {
            throw new AppException(HttpStatus.CONFLICT, "Bus was modified or deleted by another transaction");
        }
    }
    
    public boolean deleteBus(Long id) {
        try {
            if (busRepository.existsById(id)) {
                busRepository.deleteById(id);
                telegramLogService.send("🗑️ BUS DELETED: id=" + id);
                return true;
            }
            return false;
        } catch (org.hibernate.StaleObjectStateException e) {
            throw new AppException(HttpStatus.CONFLICT, "Bus was modified or deleted by another transaction");
        }
    }
}
