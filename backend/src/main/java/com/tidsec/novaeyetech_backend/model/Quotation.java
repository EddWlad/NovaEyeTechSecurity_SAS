package com.tidsec.novaeyetech_backend.model;

import com.tidsec.novaeyetech_backend.model.enums.QuotationStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cabecera de cotizacion.
 *
 * <p>Los importes son historicos: una vez guardados no se recalculan aunque cambien los parametros
 * del sistema. {@code subtotal} es la suma de bases sin IVA y {@code total} ya tiene el descuento
 * restado.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "quotations")
public class Quotation extends Auditable implements Identifiable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(nullable = false)
    private LocalDate issuedAt;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "quotations_status_enum")
    private QuotationStatus status;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercentHistorical;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal vatValueHistorical;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 8)
    private String currency;

    @Builder.Default
    @OrderBy("lineNumber ASC")
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuotationDetail> details = new ArrayList<>();

    /** Reemplaza el detalle completo manteniendo la relacion inversa consistente. */
    public void replaceDetails(List<QuotationDetail> newDetails) {
        details.clear();
        int lineNumber = 1;
        for (QuotationDetail detail : newDetails) {
            detail.setQuotation(this);
            detail.setLineNumber(lineNumber++);
            details.add(detail);
        }
    }
}
