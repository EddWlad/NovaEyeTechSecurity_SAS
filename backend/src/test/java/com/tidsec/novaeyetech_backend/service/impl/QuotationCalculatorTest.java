package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tidsec.novaeyetech_backend.dto.QuotationItemRequest;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.QuotationDetail;
import com.tidsec.novaeyetech_backend.model.QuotationSetting;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reglas de calculo del motor de cotizaciones.
 *
 * <p>Son las que no se pueden alterar sin pedido explicito del negocio, asi que se fijan aqui: si
 * alguien invierte el orden IVA/ganancia o vuelve a prohibir el margen 0, estas pruebas fallan.
 */
@ExtendWith(MockitoExtension.class)
class QuotationCalculatorTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();

    @Mock
    private IProductRepo productRepo;

    @Mock
    private IServiceItemRepo serviceRepo;

    private QuotationCalculator calculator;
    private QuotationSetting settings;

    @BeforeEach
    void setUp() {
        calculator = new QuotationCalculator(productRepo, serviceRepo);
        settings = QuotationSetting.builder()
                .currentVat(new BigDecimal("15.00"))
                .allowedVatRates(List.of("0", "1", "12", "15"))
                .allowedMargins(List.of("0", "10", "12", "20", "25", "30"))
                .defaultMargin(new BigDecimal("20.00"))
                .defaultCurrency("USD")
                .defaultValidityDays(15)
                .build();
    }

    @Test
    @DisplayName("aplica primero el IVA y despues la ganancia")
    void appliesVatBeforeMargin() {
        givenProductCosting("85.00");

        QuotationCalculator.Calculation calculation =
                calculator.calculate(List.of(productItem("4", null, null)), settings);

        QuotationDetail detail = calculation.details().getFirst();

        // 85 * 1.15 * 1.20 = 117.30. El orden inverso daria 117.30 tambien por conmutatividad,
        // pero el IVA de linea (85 * 0.15 * 4 = 51.00) solo cuadra si se calcula sobre la base.
        assertThat(detail.getUnitPriceFinal()).isEqualByComparingTo("117.30");
        assertThat(detail.getLineVatValue()).isEqualByComparingTo("51.00");
        assertThat(detail.getLineSubtotalBase()).isEqualByComparingTo("340.00");
        assertThat(detail.getLineTotal()).isEqualByComparingTo("469.20");
    }

    @Test
    @DisplayName("acepta margen 0 para servicios con precio cerrado")
    void acceptsZeroMargin() {
        givenServiceCosting("35.00");

        QuotationCalculator.Calculation calculation =
                calculator.calculate(List.of(serviceItem("4", BigDecimal.ZERO)), settings);

        assertThat(calculation.details().getFirst().getUnitPriceFinal()).isEqualByComparingTo("40.25");
        assertThat(calculation.grossTotal()).isEqualByComparingTo("161.00");
    }

    @Test
    @DisplayName("suma el subtotal sin IVA y el total bruto con IVA y margen")
    void accumulatesTotals() {
        givenProductCosting("85.00");
        givenServiceCosting("35.00");

        QuotationCalculator.Calculation calculation = calculator.calculate(
                List.of(productItem("4", null, null), serviceItem("4", BigDecimal.ZERO)), settings);

        assertThat(calculation.subtotalBase()).isEqualByComparingTo("480.00");
        assertThat(calculation.vatValueTotal()).isEqualByComparingTo("72.00");
        assertThat(calculation.grossTotal()).isEqualByComparingTo("630.20");
    }

    @Test
    @DisplayName("rechaza un porcentaje de IVA fuera de la configuracion")
    void rejectsUnknownVat() {
        givenProductCosting("85.00");

        assertThatThrownBy(() ->
                calculator.calculate(List.of(productItem("1", new BigDecimal("7"), null)), settings))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("IVA no permitido");
    }

    @Test
    @DisplayName("rechaza un margen fuera de la configuracion")
    void rejectsUnknownMargin() {
        givenProductCosting("85.00");

        assertThatThrownBy(() ->
                calculator.calculate(List.of(productItem("1", null, new BigDecimal("37"))), settings))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Margen de ganancia no permitido");
    }

    @Test
    @DisplayName("compara los porcentajes por valor: 15 y 15.00 son el mismo IVA")
    void comparesPercentagesNumerically() {
        givenProductCosting("85.00");

        QuotationCalculator.Calculation calculation =
                calculator.calculate(List.of(productItem("1", new BigDecimal("15.00"), null)), settings);

        assertThat(calculation.details()).hasSize(1);
    }

    @Test
    @DisplayName("exige productId cuando el item es de tipo PRODUCTO")
    void requiresProductId() {
        QuotationItemRequest item = new QuotationItemRequest();
        item.setItemType(QuotationItemType.PRODUCTO);
        item.setQuantity(BigDecimal.ONE);

        assertThatThrownBy(() -> calculator.calculate(List.of(item), settings))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("requiere productId");
    }

    @Test
    @DisplayName("congela la descripcion del catalogo cuando el item no trae una propia")
    void freezesCatalogDescription() {
        givenProductCosting("85.00");

        QuotationCalculator.Calculation calculation =
                calculator.calculate(List.of(productItem("1", null, null)), settings);

        assertThat(calculation.details().getFirst().getDescriptionFrozen())
                .isEqualTo("Camara domo IP 4MP con vision nocturna");
    }

    private void givenProductCosting(String baseCost) {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .name("Camara domo IP 4MP")
                .description("Camara domo IP 4MP con vision nocturna")
                .baseCost(new BigDecimal(baseCost))
                .build();

        when(productRepo.findById(any(UUID.class))).thenReturn(Optional.of(product));
    }

    private void givenServiceCosting(String baseCost) {
        ServiceItem service = ServiceItem.builder()
                .id(SERVICE_ID)
                .name("Instalacion de camara")
                .description("Instalacion y puesta en marcha por punto")
                .baseCost(new BigDecimal(baseCost))
                .build();

        when(serviceRepo.findById(any(UUID.class))).thenReturn(Optional.of(service));
    }

    private QuotationItemRequest productItem(String quantity, BigDecimal vatPercent, BigDecimal marginPercent) {
        QuotationItemRequest item = new QuotationItemRequest();
        item.setItemType(QuotationItemType.PRODUCTO);
        item.setProductId(PRODUCT_ID);
        item.setQuantity(new BigDecimal(quantity));
        item.setVatPercent(vatPercent);
        item.setMarginPercent(marginPercent);

        return item;
    }

    private QuotationItemRequest serviceItem(String quantity, BigDecimal marginPercent) {
        QuotationItemRequest item = new QuotationItemRequest();
        item.setItemType(QuotationItemType.SERVICIO);
        item.setServiceId(SERVICE_ID);
        item.setQuantity(new BigDecimal(quantity));
        item.setMarginPercent(marginPercent);

        return item;
    }
}
