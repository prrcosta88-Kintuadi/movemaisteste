package br.com.desafio.orders.orders.dto;

public record OrderItemResponse(
    String sku,
    String name,
    int quantity,
    String unitPrice,
    String subtotal
) {
}