package it.alf.twelve_factor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Application Class - 12-Factor App Demonstration
 * 
 * FACTOR I: Codebase
 * - Un'unica codebase tracciata nel version control (Git)
 * - Molti deploy possibili (dev, staging, production) dalla stessa base di codice
 * 
 * FACTOR VII: Port Binding
 * - Include Tomcat embedded (Spring Boot starter-web)
 * - L'app è self-contained e si avvia autonomamente
 * - Non richiede un application server esterno
 * 
 * FACTOR IX: Disposability
 * - Fast startup: Spring Boot ottimizzato per avvio rapido
 * - Graceful shutdown configurato in application.yml
 */
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        // FACTOR VII: L'applicazione si avvia autonomamente con embedded Tomcat
        // FACTOR IX: Startup veloce e shutdown gestito correttamente
        SpringApplication.run(Application.class, args);
    }
}
