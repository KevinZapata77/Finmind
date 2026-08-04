package com.finmind.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmind.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Devuelve 401 y 403 con el mismo formato JSON que el resto de los errores de la
 * API, en lugar de la pagina de error por defecto de Spring (API-03).
 */
@Component
public class RespuestaNoAutorizada implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespuestaNoAutorizada(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(request, response, HttpStatus.UNAUTHORIZED,
                "Se requiere un token de acceso valido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(request, response, HttpStatus.FORBIDDEN,
                "No tiene permisos para ejecutar esta accion");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus estado, String mensaje) throws IOException {
        ApiError cuerpo = new ApiError(OffsetDateTime.now(), estado.value(),
                estado.getReasonPhrase(), mensaje, request.getRequestURI(), null);
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
