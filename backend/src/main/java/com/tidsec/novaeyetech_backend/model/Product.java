package com.tidsec.novaeyetech_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "products")
public class Product extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "main_supplier_id")
    private Supplier mainSupplier;

    @Column(nullable = false, unique = true, length = 80)
    private String internalCode;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 120)
    private String brand;

    @Column(length = 120)
    private String model;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseCost;

    @Column(precision = 12, scale = 2)
    private BigDecimal stock;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false)
    private boolean active;
}
