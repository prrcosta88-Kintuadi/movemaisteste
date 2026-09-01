package br.com.desafio.orders.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitario de dinheiro.
 *
 * <p>Regra da casa: dinheiro trafega e e armazenado em <b>centavos</b> ({@code long}).
 * {@code double} e {@code float} sao proibidos para valores monetarios.
 * A conversao para o formato do contrato da API ("289.70") acontece so na borda.
 */
public final class Money {

    private Money() {
    }

    /**
     * 28970 -> "289.70"
     */
    public static String format(long cents) {
        return BigDecimal.valueOf(cents, 2).toPlainString();
    }

    /**
     * "289.70" -> 28970
     *
     * @throws IllegalArgumentException se o valor tiver mais de 2 casas decimais
     */
    public static long parse(String value) {
        try {
            return new BigDecimal(value)
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Valor monetario invalido: " + value, e);
        }
    }
}
