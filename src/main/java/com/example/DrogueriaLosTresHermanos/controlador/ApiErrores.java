package com.example.DrogueriaLosTresHermanos.controlador;

import com.example.DrogueriaLosTresHermanos.servicio.BaseDatos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Respuestas de error de la API en JSON, con mensajes que se le pueden mostrar al usuario. */
@RestControllerAdvice(assignableTypes = {ApiCuentaControlador.class, ApiAdminControlador.class})
public class ApiErrores {

    private static final Logger LOG = LoggerFactory.getLogger(ApiErrores.class);

    @ExceptionHandler(BaseDatos.ErrorUsuario.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(BaseDatos.ErrorUsuario e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(BaseDatos.NoDisponible.class)
    public ResponseEntity<Map<String, Object>> sinBase(BaseDatos.NoDisponible e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage(), "disponible", false));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException e) {
        String mensaje = e.getReason() != null ? e.getReason() : "No encontrado.";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        LOG.error("Error inesperado en la API", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Ocurrió un error. Inténtalo de nuevo en unos minutos."));
    }
}
