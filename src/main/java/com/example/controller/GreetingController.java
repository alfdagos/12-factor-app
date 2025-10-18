package com.example.controller;

import com.example.model.Greeting;
import com.example.service.GreetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller
 * Factor VII: Port binding - Esporta servizi via port binding
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GreetingController {
    
    private final GreetingService greetingService;
    
    @GetMapping("/greeting")
    public Greeting getGreeting(@RequestParam(defaultValue = "World") String name) {
        return greetingService.createGreeting(name);
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}