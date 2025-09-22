package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO : BE 비즈니스 예외 처리(일괄 적용 예정)
    // @ExceptionHandler(BusinessException.class)
    // public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
    //     log.error("Business exception occurred: {}", e.getMessage(), e);
    //     return ResponseEntity.status(e.getHttpStatus())
    //             .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
    // }

    // TODO : BE 비즈니스 예외 처리(일괄 적용 예정)
    // @ExceptionHandler(ResourceNotFoundException.class)
    // public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
    //     log.error("Resource not found: {}", e.getMessage(), e);
    //     return ResponseEntity.status(HttpStatus.NOT_FOUND)
    //             .body(ApiResponse.error(e.getMessage(), "RESOURCE_NOT_FOUND"));
    // }

    // TODO : BE 비즈니스 예외 처리(일괄 적용 예정)
    // @ExceptionHandler(ValidationException.class)
    // public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException e) {
    //     log.error("Validation exception: {}", e.getMessage(), e);
    //     HttpStatus status = e.getMessage() != null && e.getMessage().toLowerCase().contains("system config cannot be deleted")
    //             ? HttpStatus.FORBIDDEN
    //             : HttpStatus.BAD_REQUEST;
    //     return ResponseEntity.status(status)
    //             .body(ApiResponse.error(e.getMessage(), status == HttpStatus.FORBIDDEN ? "FORBIDDEN" : "VALIDATION_ERROR"));
    // }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation errors: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .errorCode("VALIDATION_ERROR")
                        .data(errors)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("Invalid request body: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid request body", "INVALID_REQUEST_BODY"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.error("Unsupported media type: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("Unsupported media type", "UNSUPPORTED_MEDIA_TYPE"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Bad request", "VALIDATION_ERROR"));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception e) {
        log.warn("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", "NOT_FOUND"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error", "INTERNAL_ERROR"));
    }
}
