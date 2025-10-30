package it.alf.twelve_factor.controller;

import it.alf.twelve_factor.model.Greeting;
import it.alf.twelve_factor.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

/**
 * Test per GreetingController
 * 
 * Dimostra Factor VII (Port Binding):
 * - Il controller espone servizi HTTP
 * - Test dell'API REST
 * - Verifica del binding su endpoint specifici
 */
@WebMvcTest(GreetingController.class)
@DisplayName("GreetingController Tests - Factor VII: Port Binding")
class GreetingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GreetingService greetingService;
    
    @Test
    @DisplayName("GET /api/v1/greeting dovrebbe restituire un greeting")
    void shouldReturnGreeting() throws Exception {
        // Given
        Greeting mockGreeting = new Greeting(1L, "Hello, Cloud!", "1.0.0");
        when(greetingService.createGreeting("Cloud")).thenReturn(mockGreeting);
        
        // When & Then
        mockMvc.perform(get("/api/v1/greeting").param("name", "Cloud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.message", is("Hello, Cloud!")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }
    
    @Test
    @DisplayName("GET /api/v1/greeting senza parametro dovrebbe usare default 'World'")
    void shouldUseDefaultNameWhenNotProvided() throws Exception {
        // Given
        Greeting mockGreeting = new Greeting(1L, "Hello, World!", "1.0.0");
        when(greetingService.createGreeting("World")).thenReturn(mockGreeting);
        
        // When & Then
        mockMvc.perform(get("/api/v1/greeting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Hello, World!")));
    }
    
    @Test
    @DisplayName("GET /api/v1/health dovrebbe restituire OK")
    void shouldReturnHealthOk() throws Exception {
        // Factor XII: Simple health check (preferire /actuator/health in produzione)
        
        // When & Then
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
