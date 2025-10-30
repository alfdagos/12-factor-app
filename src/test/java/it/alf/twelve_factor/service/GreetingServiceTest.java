package it.alf.twelve_factor.service;

import it.alf.twelve_factor.model.Greeting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per GreetingService
 * 
 * Questi test dimostrano il Factor VI (Stateless):
 * - Il service può essere testato in isolamento
 * - Nessuna dipendenza da stato esterno
 * - Risultati prevedibili basati solo su input e config
 */
@DisplayName("GreetingService Tests - Factor VI: Stateless Design")
class GreetingServiceTest {
    
    private GreetingService greetingService;
    
    @BeforeEach
    void setUp() {
        greetingService = new GreetingService();
        
        // Simula injection di configurazione (Factor III)
        ReflectionTestUtils.setField(greetingService, "greetingPrefix", "Hello");
        ReflectionTestUtils.setField(greetingService, "appVersion", "1.0.0-test");
    }
    
    @Test
    @DisplayName("Dovrebbe creare un greeting con il prefix configurato")
    void shouldCreateGreetingWithConfiguredPrefix() {
        // Given
        String name = "World";
        
        // When
        Greeting greeting = greetingService.createGreeting(name);
        
        // Then
        assertNotNull(greeting);
        assertEquals("Hello, World!", greeting.getMessage());
        assertEquals("1.0.0-test", greeting.getVersion());
        assertTrue(greeting.getId() > 0);
    }
    
    @Test
    @DisplayName("Dovrebbe incrementare il counter ad ogni chiamata")
    void shouldIncrementCounterOnEachCall() {
        // Given & When
        Greeting greeting1 = greetingService.createGreeting("First");
        Greeting greeting2 = greetingService.createGreeting("Second");
        Greeting greeting3 = greetingService.createGreeting("Third");
        
        // Then
        assertTrue(greeting2.getId() > greeting1.getId());
        assertTrue(greeting3.getId() > greeting2.getId());
    }
    
    @Test
    @DisplayName("Dovrebbe gestire nomi diversi mantenendo stato indipendente")
    void shouldHandleDifferentNamesIndependently() {
        // Factor VI: Ogni richiesta è indipendente
        
        // Given
        String[] names = {"Alice", "Bob", "Charlie"};
        
        // When & Then
        for (String name : names) {
            Greeting greeting = greetingService.createGreeting(name);
            assertTrue(greeting.getMessage().contains(name));
            assertEquals("1.0.0-test", greeting.getVersion());
        }
    }
    
    @Test
    @DisplayName("Dovrebbe usare configurazione esternalizzata (Factor III)")
    void shouldUseExternalizedConfiguration() {
        // Factor III: Config da environment
        
        // Given - Cambia configurazione a runtime
        ReflectionTestUtils.setField(greetingService, "greetingPrefix", "Ciao");
        
        // When
        Greeting greeting = greetingService.createGreeting("Mondo");
        
        // Then
        assertEquals("Ciao, Mondo!", greeting.getMessage());
    }
    
    @Test
    @DisplayName("Dovrebbe resettare il counter correttamente")
    void shouldResetCounterCorrectly() {
        // Given
        greetingService.createGreeting("Test1");
        greetingService.createGreeting("Test2");
        
        // When
        greetingService.resetCounter();
        Greeting greetingAfterReset = greetingService.createGreeting("Test3");
        
        // Then
        assertEquals(1, greetingAfterReset.getId());
    }
}
