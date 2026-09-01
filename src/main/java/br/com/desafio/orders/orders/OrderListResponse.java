package br.com.desafio.orders.orders.dto;

import java.util.List;

public record OrderListResponse(
    List<OrderSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}