package br.com.desafio.orders.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Exemplo de teste unitario puro (sem contexto Spring) - roda em milissegundos.
 * Prefira este tipo de teste para regra de negocio.
 */
class MoneyTest {

    @Test
    @DisplayName("formata centavos com duas casas decimais")
    void formataCentavos() {
        assertThat(Money.format(0L)).isEqualTo("0.00");
        assertThat(Money.format(5L)).isEqualTo("0.05");
        assertThat(Money.format(1000L)).isEqualTo("10.00");
        assertThat(Money.format(28970L)).isEqualTo("289.70");
    }

    @Test
    @DisplayName("converte reais em centavos")
    void converteReaisEmCentavos() {
        assertThat(Money.parse("0.00")).isZero();
        assertThat(Money.parse("289.70")).isEqualTo(28970L);
        assertThat(Money.parse("10")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("recusa valor com mais de duas casas decimais")
    void recusaValorComMaisDeDuasCasas() {
        assertThatThrownBy(() -> Money.parse("1.234"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
