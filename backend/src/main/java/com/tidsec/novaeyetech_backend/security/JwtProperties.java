package com.tidsec.novaeyetech_backend.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del token. El secreto nunca tiene valor por defecto: si falta la variable de entorno
 * la aplicacion no arranca, en lugar de firmar con una clave conocida.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
