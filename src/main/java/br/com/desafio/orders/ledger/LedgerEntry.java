package br.com.desafio.orders.ledger;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Razao (extrato) da carteira: uma linha para cada movimento de dinheiro.
 *
 * <p>Toda alteracao de saldo tem que deixar rastro aqui, na MESMA transacao que
 * altera o saldo. Nao existe endpoint de extrato neste desafio - a tabela
 * {@code ledger_entry} e conferida direto no banco pela bateria de aceite.
 *
 * <p>Invariante: {@code saldo_atual == saldo_inicial - soma(DEBIT) + soma(REFUND)}.
 */
@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "order_id")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private LedgerEntryType type;

    /** Valor SEMPRE positivo, em centavos. O sinal e dado pelo {@link #type}. */
    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {
        // exigido pelo JPA
    }

    public LedgerEntry(UUID walletId, UUID orderId, LedgerEntryType type, long amountCents, Instant createdAt) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Valor do lancamento deve ser positivo: " + amountCents);
        }
        this.walletId = walletId;
        this.orderId = orderId;
        this.type = type;
        this.amountCents = amountCents;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
