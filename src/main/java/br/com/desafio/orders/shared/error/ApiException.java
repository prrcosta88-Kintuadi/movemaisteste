package br.com.desafio.orders.shared.error;

/**
 * Excecao de negocio. O {@link GlobalExceptionHandler} traduz para o corpo de erro
 * padrao e para o HTTP status declarado no {@link ErrorCode}.
 *
 * <p>Uso: {@code throw new ApiException(ErrorCode.WALLET_NOT_FOUND, "Carteira ... nao encontrada.");}
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ErrorCode code;

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
