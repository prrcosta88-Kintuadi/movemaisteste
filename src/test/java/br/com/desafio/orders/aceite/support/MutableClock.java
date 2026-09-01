package br.com.desafio.orders.aceite.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Relógio controlável usado pela bateria de aceite para simular a passagem do tempo.
 *
 * <p>Ele anda junto com o relógio real, mas com um deslocamento que o teste controla.
 * Só funciona se a sua regra de negócio ler o instante atual do bean {@link Clock}
 * (ex.: {@code Instant.now(clock)}) em vez de chamar {@code Instant.now()} direto.
 */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock() {
        this(ZoneOffset.UTC);
    }

    public MutableClock(ZoneId zone) {
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId newZone) {
        MutableClock copy = new MutableClock(newZone);
        copy.offset = this.offset;
        return copy;
    }

    @Override
    public Instant instant() {
        return Instant.now().plus(offset);
    }

    /** Empurra o "agora" para frente. */
    public void advance(Duration duration) {
        this.offset = this.offset.plus(duration);
    }

    /** Volta o relógio a acompanhar o tempo real. */
    public void reset() {
        this.offset = Duration.ZERO;
    }
}
