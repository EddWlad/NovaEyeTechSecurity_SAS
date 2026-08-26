package com.tidsec.novaeyetech_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos de la empresa que se imprimen en la cabecera del PDF de cotizacion. */
@ConfigurationProperties(prefix = "app.company")
public record CompanyProperties(
        String name,
        String tagline,
        String taxId,
        String address,
        String phone
) {
}
