package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PolicyEvaluationRequest DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("PolicyEvaluationRequest DTO 테스트")
class PolicyEvaluationRequestTest {
    
    private PolicyEvaluationRequest request;
    
    @BeforeEach
    void setUp() {
        request = PolicyEvaluationRequest.builder()
                .resourceType("EC2_INSTANCE")
                .resourceId("i-1234567890abcdef0")
                .action("CREATE")
                .userId("user123")
                .tenantKey("tenant1")
                .clientIp("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .timestamp(LocalDateTime.now())
                .requestId("req-12345")
                .sessionId("session-67890")
                .requestPath("/api/resources")
                .httpMethod("POST")
                .source("WEB_UI")
                .priority(10)
                .build();
    }
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("기본 필드로 객체 생성")
        void builder_BasicFields_CreatesObject() {
            // When
            PolicyEvaluationRequest result = PolicyEvaluationRequest.builder()
                    .resourceType("S3_BUCKET")
                    .resourceId("my-bucket")
                    .action("DELETE")
                    .userId("user456")
                    .timestamp(LocalDateTime.now())
                    .build();
            
            // Then
            assertThat(result.getResourceType()).isEqualTo("S3_BUCKET");
            assertThat(result.getResourceId()).isEqualTo("my-bucket");
            assertThat(result.getAction()).isEqualTo("DELETE");
            assertThat(result.getUserId()).isEqualTo("user456");
            assertThat(result.getTimestamp()).isNotNull();
            assertThat(result.getPriority()).isEqualTo(0); // 기본값
        }
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusHours(1);
            
            Map<String, Object> context = new HashMap<>();
            context.put("userRole", "ADMIN");
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer token");
            
            // When
            PolicyEvaluationRequest result = PolicyEvaluationRequest.builder()
                    .resourceType("RDS_DATABASE")
                    .resourceId("db-instance-1")
                    .action("START")
                    .userId("admin123")
                    .tenantKey("tenant-premium")
                    .clientIp("10.0.0.1")
                    .userAgent("CLI/1.0")
                    .context(context)
                    .timestamp(now)
                    .requestId("req-abc")
                    .sessionId("session-xyz")
                    .requestPath("/api/database/start")
                    .httpMethod("PUT")
                    .headers(headers)
                    .source("CLI")
                    .priority(100)
                    .expiresAt(expiresAt)
                    .build();
            
            // Then
            assertThat(result.getResourceType()).isEqualTo("RDS_DATABASE");
            assertThat(result.getResourceId()).isEqualTo("db-instance-1");
            assertThat(result.getAction()).isEqualTo("START");
            assertThat(result.getUserId()).isEqualTo("admin123");
            assertThat(result.getTenantKey()).isEqualTo("tenant-premium");
            assertThat(result.getClientIp()).isEqualTo("10.0.0.1");
            assertThat(result.getUserAgent()).isEqualTo("CLI/1.0");
            assertThat(result.getContext()).isEqualTo(context);
            assertThat(result.getTimestamp()).isEqualTo(now);
            assertThat(result.getRequestId()).isEqualTo("req-abc");
            assertThat(result.getSessionId()).isEqualTo("session-xyz");
            assertThat(result.getRequestPath()).isEqualTo("/api/database/start");
            assertThat(result.getHttpMethod()).isEqualTo("PUT");
            assertThat(result.getHeaders()).isEqualTo(headers);
            assertThat(result.getSource()).isEqualTo("CLI");
            assertThat(result.getPriority()).isEqualTo(100);
            assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        }
    }
    
    @Nested
    @DisplayName("만료 확인 테스트")
    class ExpirationTest {
        
        @Test
        @DisplayName("만료 시간이 없으면 만료되지 않음")
        void isExpired_NoExpiration_ReturnsFalse() {
            // Given
            request.setExpiresAt(null);
            
            // When
            boolean result = request.isExpired();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("만료 시간이 미래면 만료되지 않음")
        void isExpired_FutureExpiration_ReturnsFalse() {
            // Given
            request.setExpiresAt(LocalDateTime.now().plusHours(1));
            
            // When
            boolean result = request.isExpired();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("만료 시간이 과거면 만료됨")
        void isExpired_PastExpiration_ReturnsTrue() {
            // Given
            request.setExpiresAt(LocalDateTime.now().minusHours(1));
            
            // When
            boolean result = request.isExpired();
            
            // Then
            assertThat(result).isTrue();
        }
    }
    
    @Nested
    @DisplayName("컨텍스트 관리 테스트")
    class ContextManagementTest {
        
        @Test
        @DisplayName("컨텍스트 값 가져오기")
        void getContextValue_ExistingKey_ReturnsValue() {
            // Given
            request.setContextValue("userRole", "ADMIN");
            
            // When
            Object result = request.getContextValue("userRole");
            
            // Then
            assertThat(result).isEqualTo("ADMIN");
        }
        
        @Test
        @DisplayName("존재하지 않는 컨텍스트 값 가져오기")
        void getContextValue_NonExistingKey_ReturnsNull() {
            // When
            Object result = request.getContextValue("nonExistingKey");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("컨텍스트가 null일 때 값 가져오기")
        void getContextValue_NullContext_ReturnsNull() {
            // Given
            request.setContext(null);
            
            // When
            Object result = request.getContextValue("anyKey");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("컨텍스트 값 설정")
        void setContextValue_NewValue_SetsValue() {
            // When
            request.setContextValue("department", "Engineering");
            
            // Then
            assertThat(request.getContextValue("department")).isEqualTo("Engineering");
        }
        
        @Test
        @DisplayName("null 컨텍스트에 값 설정")
        void setContextValue_NullContext_CreatesContextAndSetsValue() {
            // Given
            request.setContext(null);
            
            // When
            request.setContextValue("key", "value");
            
            // Then
            assertThat(request.getContext()).isNotNull();
            assertThat(request.getContextValue("key")).isEqualTo("value");
        }
    }
    
    @Nested
    @DisplayName("헤더 관리 테스트")
    class HeaderManagementTest {
        
        @Test
        @DisplayName("헤더 값 가져오기")
        void getHeaderValue_ExistingKey_ReturnsValue() {
            // Given
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer token");
            request.setHeaders(headers);
            
            // When
            String result = request.getHeaderValue("Authorization");
            
            // Then
            assertThat(result).isEqualTo("Bearer token");
        }
        
        @Test
        @DisplayName("존재하지 않는 헤더 값 가져오기")
        void getHeaderValue_NonExistingKey_ReturnsNull() {
            // When
            String result = request.getHeaderValue("nonExistingHeader");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("헤더가 null일 때 값 가져오기")
        void getHeaderValue_NullHeaders_ReturnsNull() {
            // Given
            request.setHeaders(null);
            
            // When
            String result = request.getHeaderValue("anyHeader");
            
            // Then
            assertThat(result).isNull();
        }
    }
    
    @Nested
    @DisplayName("파라미터 관리 테스트")
    class ParameterManagementTest {
        
        @Test
        @DisplayName("파라미터 값 가져오기")
        void getParameterValue_ExistingKey_ReturnsValue() {
            // Given
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("instanceType", "t2.micro");
            request.setParameters(parameters);
            
            // When
            Object result = request.getParameterValue("instanceType");
            
            // Then
            assertThat(result).isEqualTo("t2.micro");
        }
        
        @Test
        @DisplayName("존재하지 않는 파라미터 값 가져오기")
        void getParameterValue_NonExistingKey_ReturnsNull() {
            // When
            Object result = request.getParameterValue("nonExistingParam");
            
            // Then
            assertThat(result).isNull();
        }
        
        @Test
        @DisplayName("파라미터가 null일 때 값 가져오기")
        void getParameterValue_NullParameters_ReturnsNull() {
            // Given
            request.setParameters(null);
            
            // When
            Object result = request.getParameterValue("anyParam");
            
            // Then
            assertThat(result).isNull();
        }
    }
    
    @Nested
    @DisplayName("요청 ID 생성 테스트")
    class RequestIdGenerationTest {
        
        @Test
        @DisplayName("요청 ID가 없을 때 생성")
        void generateRequestId_NoId_GeneratesId() {
            // Given
            request.setRequestId(null);
            
            // When
            String result = request.generateRequestId();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result).isNotEmpty();
            assertThat(result).contains(request.getResourceType());
            assertThat(result).contains(request.getAction());
            assertThat(result).contains(request.getUserId());
        }
        
        @Test
        @DisplayName("요청 ID가 있을 때 기존 ID 반환")
        void generateRequestId_ExistingId_ReturnsExistingId() {
            // Given
            String existingId = "existing-request-id";
            request.setRequestId(existingId);
            
            // When
            String result = request.generateRequestId();
            
            // Then
            assertThat(result).isEqualTo(existingId);
        }
    }
    
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {
        
        @Test
        @DisplayName("toString 메서드가 올바른 형식으로 출력")
        void toString_ReturnsFormattedString() {
            // When
            String result = request.toString();
            
            // Then
            assertThat(result).contains("EC2_INSTANCE");
            assertThat(result).contains("i-1234567890abcdef0");
            assertThat(result).contains("CREATE");
            assertThat(result).contains("user123");
            assertThat(result).contains("tenant1");
        }
    }
}

