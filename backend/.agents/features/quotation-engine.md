# Motor de cotizaciones

## Resumen

Núcleo del negocio. Convierte una lista de productos y servicios del catálogo en una cotización con importes congelados y un PDF imprimible.

| Archivo | Rol |
|---|---|
| `service/impl/QuotationCalculator.java` | Cálculo puro: resuelve el origen, valida porcentajes, produce las líneas y los totales |
| `service/impl/QuotationServiceImpl.java` | Orquestación: cliente, autor, numeración, descuento, vigencia, estado, auditoría |
| `service/impl/QuotationSettingServiceImpl.java` | Fila única de configuración con IVA y márgenes permitidos |
| `util/QuotationPdfGenerator.java` | Vista PDF. Solo imprime, nunca recalcula |
| `util/pdf/PdfCanvas.java` | Dibujo sobre PDFBox con origen arriba a la izquierda |

## Reglas que no se cambian sin pedido explícito

1. **Primero IVA, después ganancia.**
   `unitPriceFinal = baseCost * (1 + iva/100) * (1 + margen/100)`
   El IVA de línea se calcula sobre la base: `baseCost * iva/100 * cantidad`.
2. **Snapshot histórico.** Cada `QuotationDetail` congela descripción, precio base, % IVA, % margen, precio unitario y totales de línea. Cambiar el catálogo o la configuración **no** altera una cotización ya guardada.
3. **El margen 0 es válido.** Cubre servicios con precio ya cerrado, como la instalación. No reintroducir validaciones de "margen > 0".
4. IVA y márgenes válidos salen de `quotation_settings`. La comparación es **numérica**: `15`, `15.0` y `15.00` son el mismo porcentaje.
5. El descuento se valida contra el **total bruto** y se resta al final. El `subtotal` almacenado es la suma de bases **sin** IVA.
6. Numeración `COT-<año>-<secuencia de 6 dígitos>`, derivada de `count()`.
7. Solo se edita una cotización en estado `BORRADOR`.
8. Un `TECNICO` solo ve y edita las suyas. Un id ajeno responde **404, no 403**.

Estas reglas están fijadas en `src/test/java/.../QuotationCalculatorTest.java`: si alguien invierte el orden IVA/ganancia o vuelve a prohibir el margen 0, las pruebas fallan.

## Dinero

Todos los importes son `BigDecimal` con escala 2 y redondeo `HALF_UP`, normalizados por `MoneyUtils`. Los acumuladores suman sin redondear y solo se redondea al persistir, para no arrastrar el error de cada línea.

En los DTOs se serializan **como String** (`@JsonFormat(shape = STRING)`): es el formato que el frontend ya recibía de las columnas `numeric`. Nunca usar `double` o `float` en un camino monetario.

## PDF

Se genera con **Apache PDFBox**, no con JasperReports: la maqueta original de `pdfkit` es imperativa (rectángulos y texto en coordenadas) y traduce casi uno a uno a PDFBox. `PdfCanvas` concentra la conversión de coordenadas, el ajuste de texto por ancho y el saneado a WinAnsi de las fuentes Standard 14.

El logo vive en `src/main/resources/assets/logo-pdf.png` y es opcional: si falta, la cabecera cae al texto de la marca. Los datos de la empresa salen de `app.company.*`, no están incrustados en el código.

El subtotal impreso es la suma de totales de línea (con IVA y margen), que no coincide con el `subtotal` almacenado (bases sin IVA). Es intencional: el documento muestra al cliente el precio de venta, la entidad guarda la base contable.
