package com.tidsec.novaeyetech_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
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
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Linea de cotizacion congelada.
 *
 * <p>Cada campo {@code *Historical} es una foto del momento en que se emitio la cotizacion: cambiar
 * el catalogo o los parametros del sistema no debe alterar una cotizacion ya guardada.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "quotation_details")
public class QuotationDetail implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private Quotation quotation;

    /** Orden estable de la linea dentro de la cotizacion. */
    @Column(nullable = false)
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "quotation_details_itemtype_enum")
    private QuotationItemType itemType;

    /** Id del producto o servicio de origen. Se guarda plano: el catalogo puede cambiar o desaparecer. */
    @Column(nullable = false, length = 80)
    private String referenceId;

    @Column(nullable = false, length = 255)
    private String descriptionFrozen;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basePriceHistorical;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentHistorical;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal marginPercentHistorical;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPriceFinal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineSubtotalBase;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineVatValue;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
