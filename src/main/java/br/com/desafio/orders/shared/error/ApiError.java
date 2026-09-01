package br.com.desafio.orders.shared.error;

import java.time.Instant;
import java.util.List;

/**
 * Corpo padrao de erro da API.
 *
 * <pre>
 * {
 *   "timestamp": "2026-08-31T17:04:11.512Z",
 *   "status": 422,
 *   "code": "INSUFFICIENT_BALANCE",
 *   "message": "Saldo insuficiente para concluir o pedido.",
 *   "path": "/api/v1/orders"
 * }
 * </pre>
 *
 * {@code details} so aparece em erros de validacao (campos nulos sao omitidos).
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> details) {

    public record FieldError(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message, String path) {
        return new ApiError(Instant.now(), code.getStatus().value(), code.name(), message, path, null);
    }

    public static ApiError of(ErrorCode code, String message, String path, List<FieldError> details) {
        return new ApiError(Instant.now(), code.getStatus().value(), code.name(), message, path, details);
    }
}
