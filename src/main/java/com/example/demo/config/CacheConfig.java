package com.example.demo.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Создаем кэши с правильными именами
        List<ConcurrentMapCache> caches = Arrays.asList(
            new ConcurrentMapCache("buses"),
            new ConcurrentMapCache("bus"),
            new ConcurrentMapCache("sensorData"),
            new ConcurrentMapCache("alerts")
        );
        
        cacheManager.setCaches(caches);
        return cacheManager;
    }
}