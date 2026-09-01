package br.com.desafio.orders.orders.dto;

import java.util.List;

public record OrderResponse(
    String id,
    String walletId,
    String status,
    String total,
    List<OrderItemResponse> items,
    String createdAt
) {
}