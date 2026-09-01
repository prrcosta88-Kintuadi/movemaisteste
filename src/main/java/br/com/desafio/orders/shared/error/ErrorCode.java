package br.com.desafio.orders.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Codigos de erro do contrato da API.
 *
 * <p>O campo {@code code} da resposta de erro é exatamente o nome da constante.
 * A bateria de aceite valida o par (HTTP status, code). Nao renomeie as constantes.
 */
public enum ErrorCode {

    /** Payload / query string / header invalido. */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** Mesma Idempotency-Key usada com um payload diferente. */
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT),

    /** Requisicao bem formada, mas a regra de negocio recusa. */
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY),
    PRODUCT_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY),
    CANCELLATION_WINDOW_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
