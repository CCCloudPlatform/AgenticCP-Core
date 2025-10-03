package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.dto.BaseErrorCode;
import com.agenticcp.core.common.enums.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Order(1)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorCodeRegistry errorCodeRegistry;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusinessException(BusinessException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        
        // Security 도메인 예외에 대한 특별한 로깅
        if (errorCode.getCode().startsWith("SECURITY_")) {
            log.warn("Security BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        } else {
            log.warn("BusinessException Caused by: {}, Message: {}", e.getClass().getSimpleName(), e.getMessage());
        }
        
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException e) {
        log.error("DataAccessException: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.DATABASE_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.BAD_REQUEST));
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        var fieldError = new ApiResponse.FieldErrorResponse(e.getParameterName(), null, "필수 요청 파라미터가 누락되었습니다.");
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, List.of(fieldError));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        var fieldError = new ApiResponse.FieldErrorResponse(e.getName(), e.getValue(), "값 타입이 올바르지 않습니다.");
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, List.of(fieldError));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        var fields = e.getConstraintViolations().stream()
                .map(v -> new ApiResponse.FieldErrorResponse(v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
                .toList();
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, fields);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("MethodArgumentNotValidException: {}", e.getMessage());
        List<ApiResponse.FieldErrorResponse> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    String code = error.getDefaultMessage();
                    BaseErrorCode baseErrorCode = errorCodeRegistry.get(code);

                    if (baseErrorCode == null) {
                        log.error("Validation code not found in ErrorCodeRegistry: {}", code);
                        return new ApiResponse.FieldErrorResponse(
                                error.getField(),
                                error.getRejectedValue(),
                                CommonErrorCode.VALIDATION_CODE_NOT_FOUND.getMessage()
                        );
                    }

                    return new ApiResponse.FieldErrorResponse(
                            error.getField(),
                            error.getRejectedValue(),
                            baseErrorCode.getMessage()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.FIELD_VALIDATION_ERROR, fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(CommonErrorCode.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(CommonErrorCode.UNAUTHORIZED, e.getMessage()));
    }

    private ResponseEntity<Object> buildFieldErrorsResponse(BaseErrorCode errorCode, List<ApiResponse.FieldErrorResponse> fieldErrors) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, fieldErrors));
    }
}
