package com.example.service;

import com.example.model.Greeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Business Logic Service
 * Factor VI: Processes - Esegui l'app come uno o più processi stateless
 */
@Service
@Slf4j
public class GreetingService {
    
    // Factor III: Config - Memorizza la config nell'environment
    @Value("${app.greeting.prefix:Hello}")
    private String greetingPrefix;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    private final AtomicLong counter = new AtomicLong();
    
    public Greeting createGreeting(String name) {
        long count = counter.incrementAndGet();
        String message = String.format("%s, %s!", greetingPrefix, name);
        
        // Factor XI: Logs - Tratta i log come event streams
        log.info("Greeting created: {} (count: {})", message, count);
        
        return new Greeting(count, message, appVersion);
    }
}