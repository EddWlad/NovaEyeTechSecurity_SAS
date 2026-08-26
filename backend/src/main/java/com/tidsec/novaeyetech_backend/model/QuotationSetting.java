package com.tidsec.novaeyetech_backend.model;

import com.tidsec.novaeyetech_backend.util.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fila unica de configuracion del motor de cotizaciones.
 * Los porcentajes permitidos se guardan como CSV, igual que el `simple-array` original.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "quotation_settings")
public class QuotationSetting extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal currentVat;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> allowedVatRates;

    /** El margen 0 es valido a proposito (servicios con precio ya cerrado): no filtrarlo. */
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> allowedMargins;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultMargin;

    @Column(nullable = false, length = 8)
    private String defaultCurrency;

    @Column(nullable = false)
    private Integer defaultValidityDays;
}
