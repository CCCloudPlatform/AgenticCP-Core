package com.agenticcp.core.domain.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 정책 평가 요청 데이터 전송 객체
 * 
 * <p>정책 엔진에 전달되는 요청 정보를 담는 DTO입니다.
 * 정책 평가에 필요한 모든 컨텍스트 정보를 포함합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyEvaluationRequest {
    
    /**
     * 리소스 타입
     * 예: "EC2_INSTANCE", "S3_BUCKET", "RDS_DATABASE"
     */
    @NotBlank(message = "리소스 타입은 필수입니다")
    private String resourceType;
    
    /**
     * 리소스 ID
     * 예: "i-1234567890abcdef0", "my-bucket", "my-database"
     */
    @NotBlank(message = "리소스 ID는 필수입니다")
    private String resourceId;
    
    /**
     * 수행하려는 액션
     * 예: "CREATE", "READ", "UPDATE", "DELETE", "START", "STOP"
     */
    @NotBlank(message = "액션은 필수입니다")
    private String action;
    
    /**
     * 요청한 사용자 ID
     */
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    /**
     * 테넌트 키
     * 멀티 테넌트 환경에서 테넌트 식별자
     */
    private String tenantKey;
    
    /**
     * 클라이언트 IP 주소
     */
    private String clientIp;
    
    /**
     * 클라이언트 User-Agent
     */
    private String userAgent;
    
    /**
     * 추가 컨텍스트 정보
     * 정책 평가에 필요한 추가적인 메타데이터
     */
    private Map<String, Object> context;
    
    /**
     * 요청 시간
     * 정책 평가 시점
     */
    @NotNull(message = "요청 시간은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 요청 ID
     * 요청 추적을 위한 고유 식별자
     */
    private String requestId;
    
    /**
     * 세션 ID
     * 사용자 세션 식별자
     */
    private String sessionId;
    
    /**
     * 요청 경로
     * API 엔드포인트 경로
     */
    private String requestPath;
    
    /**
     * HTTP 메서드
     * GET, POST, PUT, DELETE 등
     */
    private String httpMethod;
    
    /**
     * 요청 헤더 정보
     */
    private Map<String, String> headers;
    
    /**
     * 요청 파라미터
     */
    private Map<String, Object> parameters;
    
    /**
     * 요청 본문 크기 (바이트)
     */
    private Long requestSize;
    
    /**
     * 요청 출처
     * 예: "WEB_UI", "API", "CLI", "SCHEDULER"
     */
    private String source;
    
    /**
     * 요청 우선순위
     * 높을수록 우선 처리
     */
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 요청 만료 시간
     * 이 시간 이후에는 요청이 무효화됨
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime expiresAt;
    
    /**
     * 요청이 만료되었는지 확인
     * 
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 컨텍스트에서 특정 키의 값을 가져옴
     * 
     * @param key 컨텍스트 키
     * @return 컨텍스트 값
     */
    public Object getContextValue(String key) {
        if (context == null) {
            return null;
        }
        return context.get(key);
    }
    
    /**
     * 컨텍스트에 값을 설정
     * 
     * @param key 컨텍스트 키
     * @param value 컨텍스트 값
     */
    public void setContextValue(String key, Object value) {
        if (context == null) {
            context = new java.util.HashMap<>();
        }
        context.put(key, value);
    }
    
    /**
     * 헤더에서 특정 키의 값을 가져옴
     * 
     * @param key 헤더 키
     * @return 헤더 값
     */
    public String getHeaderValue(String key) {
        if (headers == null) {
            return null;
        }
        return headers.get(key);
    }
    
    /**
     * 파라미터에서 특정 키의 값을 가져옴
     * 
     * @param key 파라미터 키
     * @return 파라미터 값
     */
    public Object getParameterValue(String key) {
        if (parameters == null) {
            return null;
        }
        return parameters.get(key);
    }
    
    /**
     * 요청의 고유 식별자 생성
     * 
     * @return 요청 고유 식별자
     */
    public String generateRequestId() {
        if (requestId == null) {
            requestId = String.format("%s_%s_%s_%d", 
                resourceType, 
                action, 
                userId, 
                System.currentTimeMillis());
        }
        return requestId;
    }
    
    @Override
    public String toString() {
        return String.format("PolicyEvaluationRequest{resourceType='%s', resourceId='%s', action='%s', userId='%s', tenantKey='%s', timestamp=%s}", 
            resourceType, resourceId, action, userId, tenantKey, timestamp);
    }
}
