package com.tidsec.novaeyetech_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Bitacora de operaciones mutantes. Se escribe explicitamente desde cada servicio. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "audit_logs")
public class AuditLog implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 80)
    private String module;

    @Column(nullable = false, length = 80)
    private String entity;

    @Column(nullable = false, length = 80)
    private String entityId;

    @Column(nullable = false, length = 30)
    private String action;

    /** Email del actor. La columna se llama app_user porque "user" es palabra reservada en Postgres. */
    @Column(name = "app_user", nullable = false, length = 180)
    private String user;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(columnDefinition = "text")
    private String payloadSummary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
