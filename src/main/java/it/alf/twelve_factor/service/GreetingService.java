package it.alf.twelve_factor.service;

import it.alf.twelve_factor.model.Greeting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Business Logic Service - 12-Factor App Demonstration
 * 
 * FACTOR III: Config
 * - Configurazione esternalizzata tramite @Value da environment variables
 * - Valori di default per sviluppo locale
 * - Nessun valore hardcoded
 * 
 * FACTOR VI: Processes
 * - Service stateless: nessuna sessione utente in memoria
 * - Counter locale OK per metriche, ma non per stato applicativo critico
 * - Ogni istanza può gestire qualsiasi richiesta
 * - Scalabile orizzontalmente
 * 
 * FACTOR XI: Logs
 * - Log su stdout/stderr tramite SLF4J
 * - Nessun file di log locale
 * - Kubernetes/Docker raccolgono automaticamente i log
 */
@Service
@Slf4j
public class GreetingService {
    
    // FACTOR III: Config - Inietta configurazione da environment
    // Sintassi: ${ENV_VAR:defaultValue}
    // - ENV_VAR: nome variabile environment
    // - defaultValue: valore se variabile non definita
    
    @Value("${app.greeting.prefix:Hello}")
    private String greetingPrefix;
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    // FACTOR VI: Counter locale è accettabile per metriche
    // NON usare per stato critico (es. sessioni utente, carrelli, ecc.)
    // Per stato persistente usare Backing Services (Factor IV)
    private final AtomicLong counter = new AtomicLong();
    
    /**
     * Crea un messaggio di saluto personalizzato
     * 
     * FACTOR VI: Metodo stateless
     * - Non dipende da stato precedente
     * - Ogni chiamata è indipendente
     * - Risultato dipende solo dagli input e dalla config
     * 
     * FACTOR VIII: Concurrency
     * - Metodo thread-safe (AtomicLong)
     * - Supporta esecuzione concorrente
     * - Scalabile orizzontalmente
     * 
     * @param name Nome per il saluto
     * @return Oggetto Greeting con messaggio personalizzato
     */
    public Greeting createGreeting(String name) {
        // FACTOR VI: Incremento counter thread-safe
        long count = counter.incrementAndGet();
        
        // FACTOR III: Usa configurazione esternalizzata
        String message = String.format("%s, %s!", greetingPrefix, name);
        
        // FACTOR XI: Log su stdout come event stream
        // Kubernetes/Docker raccolgono automaticamente
        log.info("Greeting created: {} (count: {})", message, count);
        
        return new Greeting(count, message, appVersion);
    }
    
    /**
     * Reset counter (utile per testing)
     * In produzione, considerare Factor IV (backing service come Redis)
     */
    public void resetCounter() {
        long oldValue = counter.getAndSet(0);
        log.info("Counter reset from {} to 0", oldValue);
    }
}
