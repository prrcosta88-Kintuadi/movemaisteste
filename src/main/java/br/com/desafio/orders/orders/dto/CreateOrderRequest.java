package br.com.desafio.orders.orders.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
    @NotNull(message = "walletId e obrigatorio")
    UUID walletId,

    @NotEmpty(message = "items nao pode ser vazio")
    @Valid
    List<OrderItemRequest> items
) {
}