package com.tidsec.novaeyetech_backend.model;

import com.tidsec.novaeyetech_backend.model.enums.MaintenanceStatus;
import com.tidsec.novaeyetech_backend.model.enums.MaintenanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "maintenance")
public class Maintenance extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "maintenance_type_enum")
    private MaintenanceType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "maintenance_status_enum")
    private MaintenanceStatus status;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    private LocalDate executionDate;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(nullable = false, length = 255)
    private String intervenedSystem;

    @Column(nullable = false, columnDefinition = "text")
    private String diagnosis;

    @Column(nullable = false, columnDefinition = "text")
    private String appliedSolution;

    @Column(columnDefinition = "text")
    private String observations;
}
