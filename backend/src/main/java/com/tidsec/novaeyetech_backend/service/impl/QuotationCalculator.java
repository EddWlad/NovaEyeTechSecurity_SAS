package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.QuotationItemRequest;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.QuotationDetail;
import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Nucleo de calculo de una cotizacion.
 *
 * <p>Vive aparte del servicio porque concentra las reglas que no se pueden alterar sin pedido
 * explicito del negocio:
 * <ol>
 *   <li><b>Primero IVA, despues ganancia</b>:
 *       {@code unitPriceFinal = baseCost * (1 + iva/100) * (1 + margen/100)}.</li>
 *   <li>Cada linea congela descripcion, precio base, porcentajes y totales.</li>
 *   <li>El IVA y el margen deben pertenecer a las listas permitidas de la configuracion.
 *       El margen 0 es valido a proposito y no debe filtrarse.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class QuotationCalculator {

    private final IProductRepo productRepo;
    private final IServiceItemRepo serviceRepo;

    /**
     * Resultado del calculo.
     *
     * @param subtotalBase  suma de bases sin IVA
     * @param vatValueTotal suma del IVA de todas las lineas
     * @param grossTotal    suma de totales de linea, antes de aplicar el descuento
     */
    public record Calculation(
            List<QuotationDetail> details,
            BigDecimal subtotalBase,
            BigDecimal vatValueTotal,
            BigDecimal grossTotal
    ) {
    }

    public Calculation calculate(List<QuotationItemRequest> items, QuotationSetting settings) {
        List<BigDecimal> allowedVatRates = parsePercentages(settings.getAllowedVatRates());
        List<BigDecimal> allowedMargins = parsePercentages(settings.getAllowedMargins());

        List<QuotationDetail> details = new ArrayList<>(items.size());
        BigDecimal subtotalBase = BigDecimal.ZERO;
        BigDecimal vatValueTotal = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimal.ZERO;

        for (QuotationItemRequest item : items) {
            SourceItem source = resolveSource(item);
            BigDecimal quantity = item.getQuantity();

            if (!MoneyUtils.isPositive(quantity)) {
                throw new BusinessRuleException("La cantidad debe ser mayor a 0");
            }

            BigDecimal vatPercent = item.getVatPercent() != null
                    ? item.getVatPercent()
                    : settings.getCurrentVat();
            BigDecimal marginPercent = item.getMarginPercent() != null
                    ? item.getMarginPercent()
                    : settings.getDefaultMargin();

            requireAllowed(allowedVatRates, vatPercent, "IVA no permitido: ");
            requireAllowed(allowedMargins, marginPercent, "Margen de ganancia no permitido: ");

            BigDecimal lineSubtotalBase = source.baseCost().multiply(quantity);
            BigDecimal lineVatValue = MoneyUtils.percentOf(source.baseCost(), vatPercent).multiply(quantity);

            // Regla obligatoria: primero IVA y luego ganancia.
            BigDecimal unitPriceFinal = source.baseCost()
                    .multiply(MoneyUtils.percentFactor(vatPercent))
                    .multiply(MoneyUtils.percentFactor(marginPercent));
            BigDecimal lineTotal = unitPriceFinal.multiply(quantity);

            subtotalBase = subtotalBase.add(lineSubtotalBase);
            vatValueTotal = vatValueTotal.add(lineVatValue);
            grossTotal = grossTotal.add(lineTotal);

            details.add(QuotationDetail.builder()
                    .itemType(item.getItemType())
                    .referenceId(source.referenceId())
                    .descriptionFrozen(truncateDescription(source.description()))
                    .quantity(MoneyUtils.scale(quantity))
                    .basePriceHistorical(MoneyUtils.scale(source.baseCost()))
                    .vatPercentHistorical(MoneyUtils.scale(vatPercent))
                    .marginPercentHistorical(MoneyUtils.scale(marginPercent))
                    .unitPriceFinal(MoneyUtils.scale(unitPriceFinal))
                    .lineSubtotalBase(MoneyUtils.scale(lineSubtotalBase))
                    .lineVatValue(MoneyUtils.scale(lineVatValue))
                    .lineTotal(MoneyUtils.scale(lineTotal))
                    .build());
        }

        return new Calculation(
                details,
                MoneyUtils.scale(subtotalBase),
                MoneyUtils.scale(vatValueTotal),
                MoneyUtils.scale(grossTotal));
    }

    private record SourceItem(String referenceId, String description, BigDecimal baseCost) {
    }

    private SourceItem resolveSource(QuotationItemRequest item) {
        return item.getItemType() == QuotationItemType.PRODUCTO
                ? resolveProduct(item)
                : resolveService(item);
    }

    private SourceItem resolveProduct(QuotationItemRequest item) {
        UUID productId = item.getProductId();

        if (productId == null) {
            throw new BusinessRuleException("El item de tipo PRODUCTO requiere productId");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado para cotizacion"));

        return new SourceItem(
                product.getId().toString(),
                firstNonBlank(item.getDescription(), product.getDescription(), product.getName()),
                product.getBaseCost());
    }

    private SourceItem resolveService(QuotationItemRequest item) {
        UUID serviceId = item.getServiceId();

        if (serviceId == null) {
            throw new BusinessRuleException("El item de tipo SERVICIO requiere serviceId");
        }

        ServiceItem service = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado para cotizacion"));

        return new SourceItem(
                service.getId().toString(),
                firstNonBlank(item.getDescription(), service.getDescription(), service.getName()),
                service.getBaseCost());
    }

    /** La comparacion es numerica: "15", "15.0" y "15.00" describen el mismo porcentaje. */
    private void requireAllowed(List<BigDecimal> allowed, BigDecimal value, String messagePrefix) {
        boolean permitted = allowed.stream().anyMatch(candidate -> candidate.compareTo(value) == 0);

        if (!permitted) {
            throw new BusinessRuleException(messagePrefix + value.stripTrailingZeros().toPlainString() + "%");
        }
    }

    private List<BigDecimal> parsePercentages(List<String> rawValues) {
        return rawValues.stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(BigDecimal::new)
                .toList();
    }

    /** La descripcion congelada tiene 255 caracteres de columna: se recorta antes de persistir. */
    private String truncateDescription(String description) {
        int maxLength = 255;

        return description.length() > maxLength ? description.substring(0, maxLength) : description;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }

        return "";
    }
}
