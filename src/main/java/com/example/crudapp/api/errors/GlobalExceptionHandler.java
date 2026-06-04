package com.example.crudapp.api.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.Map;

/**
 * [CHECKLIST FEATURE / DX OPTIMIZATION]
 * Unified global exception handler mapping application exceptions 
 * to clean JSON ErrorResponse schemas with diagnostic request IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                exchange.getRequest().getPath().value(),
                reqId,
                ex.getErrors()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLocking(org.springframework.orm.ObjectOptimisticLockingFailureException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                "Concurrency conflict detected: the record has been modified by another transaction",
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(res);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage(),
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
    }

    @ExceptionHandler(org.springframework.web.server.ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleWebInputException(org.springframework.web.server.ServerWebInputException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        Throwable rootCause = ex.getRootCause();
        String message = rootCause != null ? rootCause.getMessage() : ex.getReason();
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid payload format: " + message,
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(tools.jackson.core.JacksonException.class)
    public ResponseEntity<ErrorResponse> handleJacksonException(tools.jackson.core.JacksonException ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "JSON payload parsing error: " + ex.getOriginalMessage(),
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler({NumberFormatException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, ServerWebExchange exchange) {
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid parameter or input format: " + ex.getMessage(),
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled internal server error", ex);
        String reqId = getRequestId(exchange);
        ErrorResponse res = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                exchange.getRequest().getPath().value(),
                reqId,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    private String getRequestId(ServerWebExchange exchange) {
        Object attribute = exchange.getAttribute(com.example.crudapp.infrastructure.web.ReactiveRequestTracingFilter.MDC_KEY);
        return attribute != null ? attribute.toString() : exchange.getResponse().getHeaders().getFirst(com.example.crudapp.infrastructure.web.ReactiveRequestTracingFilter.REQUEST_ID_HEADER);
    }
}
