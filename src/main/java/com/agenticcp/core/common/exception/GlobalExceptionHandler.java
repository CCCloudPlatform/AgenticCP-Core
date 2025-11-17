package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.dto.exception.BaseErrorCode;
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

/**
 * 애플리케이션 전역 예외 처리기입니다.
 * 도메인 비즈니스 예외 및 공통 시스템 예외를 표준 응답으로 변환합니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
@Order(1)
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ErrorCodeRegistry errorCodeRegistry;

    /**
     * 비즈니스 예외를 도메인별 HTTP 상태 코드와 함께 응답으로 변환합니다.
     * SECURITY 카테고리 예외는 보안 감사 목적의 별도 로깅을 수행합니다.
     *
     * @param e 도메인 비즈니스 예외
     * @return 표준 {@link ApiResponse} 에러 응답
     */
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

    /**
     * 데이터 액세스 계층에서 발생한 예외를 내부 서버 오류로 변환합니다.
     *
     * @param e 데이터베이스 관련 예외
     * @return HTTP 500 응답
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccessException(DataAccessException e) {
        log.error("DataAccessException: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.DATABASE_ERROR));
    }

    /**
     * 역직렬화 실패 등 요청 본문을 읽지 못한 경우 BAD_REQUEST로 매핑합니다.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(CommonErrorCode.BAD_REQUEST));
    }

    /**
     * 지원하지 않는 HTTP 메서드 호출 시 METHOD_NOT_ALLOWED로 응답합니다.
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 필수 요청 파라미터 누락을 필드 검증 실패 응답으로 변환합니다.
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("MissingServletRequestParameterException: {}", e.getMessage());
        var fieldError = new ApiResponse.FieldErrorResponse(e.getParameterName(), null, "필수 요청 파라미터가 누락되었습니다.");
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, List.of(fieldError));
    }

    /**
     * 경로/쿼리 파라미터 타입 불일치 시 상세 필드 오류를 제공합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        var fieldError = new ApiResponse.FieldErrorResponse(e.getName(), e.getValue(), "값 타입이 올바르지 않습니다.");
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, List.of(fieldError));
    }

    /**
     * Bean Validation 제약 조건 위반을 필드 오류 목록으로 변환합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException: {}", e.getMessage());
        var fields = e.getConstraintViolations().stream()
                .map(v -> new ApiResponse.FieldErrorResponse(v.getPropertyPath().toString(), v.getInvalidValue(), v.getMessage()))
                .toList();
        return buildFieldErrorsResponse(CommonErrorCode.FIELD_VALIDATION_ERROR, fields);
    }

    /**
     * DTO 바인딩 단계에서 발생한 {@link MethodArgumentNotValidException}을
     * 검증 코드 Registry 기반 메시지로 변환합니다.
     */
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

    /**
     * 예상치 못한 예외를 내부 서버 오류로 매핑하고 전체 스택 트레이스를 기록합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR, e.getMessage()));
    }

    /**
     * Spring Security 인가 예외를 FORBIDDEN 응답으로 변환합니다.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(CommonErrorCode.FORBIDDEN, e.getMessage()));
    }

    /**
     * 인증 실패 예외를 HTTP 401 응답으로 변환합니다.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(CommonErrorCode.UNAUTHORIZED, e.getMessage()));
    }

    /**
     * 필드 오류 응답 생성을 공통화합니다.
     *
     * @param errorCode 필드 검증용 에러 코드
     * @param fieldErrors 유효성 검증 실패 목록
     * @return {@link ApiResponse} 기반 ResponseEntity
     */
    private ResponseEntity<Object> buildFieldErrorsResponse(BaseErrorCode errorCode, List<ApiResponse.FieldErrorResponse> fieldErrors) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode, fieldErrors));
    }
}
