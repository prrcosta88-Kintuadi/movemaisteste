package br.com.desafio.orders.aceite.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Substitui o bean {@code Clock} da aplicação pelo relógio controlável durante a
 * bateria de aceite.
 *
 * <p>É {@code @TestConfiguration} (e não {@code @Configuration}) de propósito: assim ela
 * fica de fora do component scan e não vaza para os contextos dos SEUS testes — só é
 * registrada onde a bateria de aceite pede explicitamente.
 */
@TestConfiguration
public class AcceptanceClockConfig {

    @Bean
    @Primary
    public MutableClock acceptanceClock() {
        return new MutableClock();
    }
}
