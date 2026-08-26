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

/**
 * Adjunto generico. No usa relaciones JPA hacia el origen: sourceEntity + sourceEntityId permiten
 * colgar evidencias de cualquier modulo sin tocar el esquema.
 *
 * <p>Los archivos viven en Cloudinary, no en el disco del servidor: {@code storagePath} es la URL
 * segura y publica del recurso, y {@code publicId} mas {@code resourceType} son lo que el proveedor
 * necesita para poder borrarlo.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "attachments")
public class Attachment implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 80)
    private String sourceEntity;

    @Column(nullable = false, length = 80)
    private String sourceEntityId;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 255)
    private String storedName;

    @Column(nullable = false, length = 120)
    private String mimeType;

    /** URL segura del recurso en Cloudinary. El nombre se conserva por compatibilidad de contrato. */
    @Column(nullable = false, length = 500)
    private String storagePath;

    /** Identificador dentro de Cloudinary. Necesario para borrar el recurso remoto. */
    @Column(length = 255)
    private String publicId;

    /** "image" para imagenes, "raw" para documentos. Determina como se borra en Cloudinary. */
    @Column(length = 20)
    private String resourceType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 180)
    private String uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
