package com.tidsec.novaeyetech_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
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
@Table(name = "clients")
public class Client extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 180)
    private String nameOrBusinessName;

    @Column(nullable = false, unique = true, length = 30)
    private String documentNumber;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 180)
    private String email;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 255)
    private String commercialReference;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(nullable = false)
    private boolean active;
}
