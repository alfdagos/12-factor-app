package it.alf.twelve_factor.controller;

import it.alf.twelve_factor.model.Greeting;
import it.alf.twelve_factor.service.GreetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller - 12-Factor App Demonstration
 * 
 * FACTOR VI: Processes
 * - Controller stateless: nessuno stato memorizzato tra richieste
 * - Usa @RequiredArgsConstructor per dependency injection (immutabile)
 * - Ogni richiesta è indipendente
 * 
 * FACTOR VII: Port Binding
 * - Espone servizi HTTP tramite porta configurabile (server.port)
 * - Self-contained: non richiede web server esterno
 * 
 * FACTOR XI: Logs
 * - Log su stdout tramite SLF4J/Logback
 * - Nessun log su file
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GreetingController {
    
    // FACTOR VI: Dependency injection immutabile, no stato mutabile
    private final GreetingService greetingService;
    
    /**
     * Endpoint principale per generare saluti
     * 
     * FACTOR VI: Metodo stateless, ogni richiesta è indipendente
     * FACTOR XI: Log della richiesta su stdout
     */
    @GetMapping("/greeting")
    public Greeting getGreeting(@RequestParam(defaultValue = "World") String name) {
        log.info("Received greeting request for name: {}", name);
        Greeting greeting = greetingService.createGreeting(name);
        log.debug("Generated greeting: {}", greeting);
        return greeting;
    }
    
    /**
     * Simple health check endpoint
     * Note: Preferire /actuator/health per health checks production-ready
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
