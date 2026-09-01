package br.com.desafio.orders.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fonte de tempo da aplicacao.
 *
 * <p>Use SEMPRE este bean para obter o instante atual (injete {@link Clock} e chame
 * {@code Instant.now(clock)} / {@code LocalDateTime.now(clock)}).
 * Nao chame {@code Instant.now()} / {@code LocalDateTime.now()} / {@code new Date()}
 * diretamente na regra de negocio.
 *
 * <p>Motivo: a bateria de aceite substitui este bean por um relogio controlavel para
 * simular a passagem do tempo (por exemplo, para testar a janela de cancelamento).
 * Regra de negocio que le o relogio do sistema direto nao tem como ser testada.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
