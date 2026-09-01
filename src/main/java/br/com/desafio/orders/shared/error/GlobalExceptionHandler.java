package br.com.desafio.orders.shared.error;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduz excecoes para o corpo de erro padrao da API.
 * Ja vem pronto no starter - so estenda se precisar de um caso novo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(ex.getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getCode().getStatus()).body(body);
    }

    /** Corpo JSON reprovado pelo Bean Validation (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException ex,
                                                         HttpServletRequest request) {
        List<ApiError.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Requisicao invalida.", request.getRequestURI(), details);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** Validacao em parametros de metodo (@Validated em @RequestParam / @PathVariable). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        List<ApiError.FieldError> details = ex.getConstraintViolations().stream()
                .map(violation -> new ApiError.FieldError(
                        String.valueOf(violation.getPropertyPath()), violation.getMessage()))
                .collect(Collectors.toList());

        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Requisicao invalida.", request.getRequestURI(), details);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** Header obrigatorio ausente (ex.: Idempotency-Key). */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex,
                                                        HttpServletRequest request) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Header obrigatorio ausente: " + ex.getHeaderName(), request.getRequestURI());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** Query param obrigatorio ausente. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest request) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Parametro obrigatorio ausente: " + ex.getParameterName(), request.getRequestURI());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** Tipo invalido em path/query (ex.: UUID mal formado). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Valor invalido para o parametro '" + ex.getName() + "'.", request.getRequestURI());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** JSON malformado ou ausente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                         HttpServletRequest request) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_ERROR,
                "Corpo da requisicao ausente ou malformado.", request.getRequestURI());
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getStatus()).body(body);
    }

    /** Rede de seguranca. Qualquer 500 aqui e bug - nao deveria acontecer nos casos previstos. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleUnexpected(RuntimeException ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        ApiError body = ApiError.of(ErrorCode.INTERNAL_ERROR,
                "Erro interno inesperado.", request.getRequestURI());
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus()).body(body);
    }
}
