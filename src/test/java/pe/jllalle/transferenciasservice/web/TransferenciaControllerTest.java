package pe.jllalle.transferenciasservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.jllalle.transferenciasservice.domain.EstadoTransferencia;
import pe.jllalle.transferenciasservice.dto.TransferenciaResultado;
import pe.jllalle.transferenciasservice.service.TransferenciaService;
import pe.jllalle.transferenciasservice.service.exception.CuentaNoEncontradaException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de "slice": solo levanta la capa web (controlador, validación, manejo de errores),
 * sin base de datos ni contexto completo de Spring. El service se reemplaza por un mock.
 * Es el equivalente en Spring MVC de lo que @WebFluxTest hace para endpoints reactivos.
 */
@WebMvcTest(TransferenciaController.class)
class TransferenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Spring Boot 4 usa Jackson 3 (tools.jackson) como serializador por defecto y ya no
    // registra un bean com.fasterxml.jackson.databind.ObjectMapper. Para la prueba solo
    // necesitamos convertir el request a JSON, así que se crea uno propio en vez de
    // depender de un bean de Spring.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransferenciaService transferenciaService;

    @Test
    void devuelve201YElResultadoCuandoLaTransferenciaEsValida() throws Exception {
        var request = new TransferenciaRequest("clave-1", 1L, 2L, new BigDecimal("100.00"));
        var resultado = new TransferenciaResultado(1L, "clave-1", EstadoTransferencia.COMPLETADA, new BigDecimal("400.00"));

        when(transferenciaService.transferir(any())).thenReturn(resultado);

        mockMvc.perform(post("/api/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.saldoOrigenResultante").value(400.00));
    }

    @Test
    void devuelve400CuandoElMontoEsCero() throws Exception {
        var request = new TransferenciaRequest("clave-2", 1L, 2L, BigDecimal.ZERO);

        mockMvc.perform(post("/api/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // RFC 7807: los campos extra de un ProblemDetail van en la raíz del JSON,
                // no anidados bajo "properties" (eso era un error de la prueba, no de la API).
                .andExpect(jsonPath("$.monto").exists());
    }

    @Test
    void devuelve400CuandoFaltaLaClaveDeIdempotencia() throws Exception {
        var request = new TransferenciaRequest(null, 1L, 2L, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelve400CuandoElCuerpoNoEsJsonValido() throws Exception {
        // Un número con guion (001-0002) no es JSON válido: debe ser 400 (culpa del
        // cliente), nunca 500. Antes de este fix caía en el manejador genérico y
        // devolvía 500 "Ocurrió un error inesperado".
        var jsonMalformado = """
                {"claveIdempotencia": "x", "cuentaOrigenId": 1, "cuentaDestinoId": 001-0002, "monto": 10}
                """;

        mockMvc.perform(post("/api/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMalformado))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devuelve404CuandoLaCuentaNoExiste() throws Exception {
        var request = new TransferenciaRequest("clave-3", 99L, 2L, new BigDecimal("10.00"));
        when(transferenciaService.transferir(any())).thenThrow(new CuentaNoEncontradaException(99L));

        mockMvc.perform(post("/api/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
