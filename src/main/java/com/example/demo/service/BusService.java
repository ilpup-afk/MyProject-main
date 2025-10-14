package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Bus;
import com.example.demo.repository.BusRepository;
import com.example.demo.specifications.BusSpecification;

@Service
@Transactional(readOnly = true)
public class BusService {
    private final BusRepository busRepository;

    public BusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    @Cacheable(value = "buses", key = "#root.methodName")  // Изменил на "buses" для множественного числа
    public List<Bus> getAll() {
        return busRepository.findAll();
    }

    @Cacheable(value = "bus", key = "#id")
    public Bus getById(Long id) {
        return busRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)  // Очищаем оба кэша
    public Bus create(Bus bus) {
        return busRepository.save(bus);
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)  // Исправлено дублирование
    public Bus updateById(Long id, Bus updatedBus) {
        return busRepository.findById(id)
                .map(bus -> {
                    bus.setModel(updatedBus.getModel());
                    // lastReadings не обновляем - они управляются автоматически
                    return busRepository.save(bus);
                })
                .orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"buses", "bus"}, allEntries = true)  // Исправлено дублирование
    public boolean deleteById(Long id) {
        if (busRepository.existsById(id)) {
            busRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Bus> getByModel(String model) {
        if (model != null) {
            return busRepository.findByModelContainingIgnoreCase(model);
        }
        return busRepository.findAll();
    }

    public Page<Bus> getByFilter(String model, Pageable pageable) {
        return busRepository.findAll(
            BusSpecification.filterByModel(model),
            pageable);
    }
}    
   