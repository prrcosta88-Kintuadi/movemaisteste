package br.com.desafio.orders.orders.dto;

public record OrderSummaryResponse(
    String id,
    String walletId,
    String status,
    String total,
    String createdAt
) {
}