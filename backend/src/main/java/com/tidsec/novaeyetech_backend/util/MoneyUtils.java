package com.tidsec.novaeyetech_backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Normalizacion monetaria del sistema.
 *
 * <p>Todos los importes viven con escala 2 y redondeo HALF_UP. El backend NestJS resolvia esto con
 * {@code Number(...)} + {@code toFixed(2)} sobre columnas {@code numeric}; aqui la escala se fija en
 * un unico lugar para que ningun calculo nuevo introduzca desviaciones de centavos.
 */
public final class MoneyUtils {

    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MoneyUtils() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP) : scale(value);
    }

    /** Convierte un porcentaje (15) en su factor multiplicador (1.15). */
    public static BigDecimal percentFactor(BigDecimal percent) {
        return BigDecimal.ONE.add(percent.divide(HUNDRED, 10, RoundingMode.HALF_UP));
    }

    /** Aplica un porcentaje sobre una base (base * percent / 100), sin redondear todavia. */
    public static BigDecimal percentOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(HUNDRED, 10, RoundingMode.HALF_UP);
    }

    public static boolean isNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }
}
