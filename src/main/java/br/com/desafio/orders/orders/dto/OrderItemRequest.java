package br.com.desafio.orders.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record OrderItemRequest(
    @NotBlank(message = "sku e obrigatorio")
    String sku,

    @NotNull(message = "quantity e obrigatoria")
    @Min(value = 1, message = "quantity deve ser entre 1 e 100")
    @Max(value = 100, message = "quantity deve ser entre 1 e 100")
    Integer quantity
) {
}