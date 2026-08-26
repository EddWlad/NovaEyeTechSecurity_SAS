package com.tidsec.novaeyetech_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registro unico de las propiedades tipadas de la aplicacion. */
@Configuration
@EnableConfigurationProperties({CloudinaryProperties.class, CompanyProperties.class})
public class ApplicationPropertiesConfig {
}
