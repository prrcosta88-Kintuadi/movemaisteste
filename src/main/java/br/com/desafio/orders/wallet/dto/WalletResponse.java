package br.com.desafio.orders.wallet.dto;

/**
 * {@code { "id": "...", "ownerName": "Ana Souza", "balance": "500.00" }}
 *
 * <p>Note que o valor monetario sai como string com 2 casas decimais.
 * Siga esse padrao nos DTOs que voce criar.
 */
public record WalletResponse(String id, String ownerName, String balance) {
}
