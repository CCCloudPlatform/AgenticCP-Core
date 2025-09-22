package com.agenticcp.core.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

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
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
    
    public static <T> ApiResponse<T> error(BaseErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> error(BaseErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode.getCode())
                .message(message)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(null)
                .build();
    }
    
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
