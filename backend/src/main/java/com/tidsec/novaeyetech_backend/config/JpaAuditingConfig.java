package com.tidsec.novaeyetech_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Activa createdAt/updatedAt automaticos en las entidades que extienden Auditable. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
