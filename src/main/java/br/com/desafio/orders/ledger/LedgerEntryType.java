package br.com.desafio.orders.ledger;

public enum LedgerEntryType {

    /** Saida de dinheiro da carteira (pagamento do pedido). */
    DEBIT,

    /** Entrada de dinheiro na carteira (estorno de um pedido cancelado). */
    REFUND
}
