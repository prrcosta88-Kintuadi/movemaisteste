package br.com.desafio.orders.wallet;

import java.util.UUID;

import br.com.desafio.orders.shared.error.ApiException;
import br.com.desafio.orders.shared.error.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Carteira pre-paga.
 *
 * <p>Ja vem pronta no starter, mas voce PODE altera-la se o seu desenho precisar
 * (ela e um arquivo do projeto como qualquer outro).
 *
 * <p>O saldo e mantido em centavos. As operacoes de debito/credito sao metodos da
 * propria entidade para que a invariante "saldo nunca fica negativo" viva em um
 * lugar so.
 */
@Entity
@Table(name = "wallet")
public class Wallet {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    /** Saldo em centavos. */
    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    protected Wallet() {
        // exigido pelo JPA
    }

    public Wallet(UUID id, String ownerName, long balanceCents) {
        this.id = id;
        this.ownerName = ownerName;
        this.balanceCents = balanceCents;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public boolean hasSufficientBalance(long amountCents) {
        return balanceCents >= amountCents;
    }

    /**
     * Debita a carteira.
     *
     * @throws ApiException com {@link ErrorCode#INSUFFICIENT_BALANCE} se o saldo nao cobre o valor
     */
    public void debit(long amountCents) {
        requirePositive(amountCents);
        if (!hasSufficientBalance(amountCents)) {
            throw new ApiException(ErrorCode.INSUFFICIENT_BALANCE,
                    "Saldo insuficiente para concluir o pedido.");
        }
        this.balanceCents -= amountCents;
    }

    /** Credita a carteira (usado no estorno de um cancelamento). */
    public void credit(long amountCents) {
        requirePositive(amountCents);
        this.balanceCents += amountCents;
    }

    private static void requirePositive(long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo: " + amountCents);
        }
    }
}
