package com.agenticcp.core.common.dto.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * API 표준 응답 포맷을 제공하는 제네릭 DTO입니다.
 * 성공/실패 여부, 메시지, 데이터, 에러 코드, 필드 오류, 타임스탬프를 포함합니다.
 * 
 * @param <T> 응답 데이터 타입
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    
    private final boolean success;
    private final String message;
    private final T data;
    private final String errorCode;
    private final List<FieldErrorResponse> fieldErrors;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final OffsetDateTime timestamp;
    
    /**
     * payload만 있는 성공 응답을 생성합니다.
     *
     * @param data 성공 시 반환할 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
    
    /**
     * 메시지와 payload를 포함한 성공 응답을 생성합니다.
     *
     * @param data    성공 시 반환할 데이터
     * @param message 사용자 정의 성공 메시지
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
    
    /**
     * 기본 오류 응답을 생성합니다.
     *
     * @param errorCode 비즈니스 에러 코드
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> error(BaseErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(null)
                .build();
    }

    /**
     * 기본 메시지를 커스터마이징한 오류 응답을 생성합니다.
     *
     * @param errorCode 비즈니스 에러 코드
     * @param message   사용자 정의 오류 메시지
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> error(BaseErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(message)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(null)
                .build();
    }
    
    /**
     * 필드 오류 목록을 포함한 오류 응답을 생성합니다.
     *
     * @param errorCode   비즈니스 에러 코드
     * @param fieldErrors 검증 실패 상세 목록
     * @return 실패 응답
     */
    public static <T> ApiResponse<T> error(BaseErrorCode errorCode, List<FieldErrorResponse> fieldErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .fieldErrors(fieldErrors)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(null)
                .build();
    }
    
    public record FieldErrorResponse(
            String field,
            Object value,
            String reason
    ) {}
}
