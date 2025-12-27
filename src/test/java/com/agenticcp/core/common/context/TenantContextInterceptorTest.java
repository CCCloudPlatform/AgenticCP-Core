package com.agenticcp.core.common.context;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * TenantContextInterceptor 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantContextInterceptor 단위 테스트")
class TenantContextInterceptorTest {

    @Mock
    private TenantContextService tenantContextService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TenantContextInterceptor interceptor;

    private User testUser;
    private Tenant testTenant;
    private Worker testWorker;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .build();
        testUser.setId(1L);

        testTenant = Tenant.builder()
                .tenantKey("tenant-1")
                .tenantName("Tenant 1")
                .build();
        testTenant.setId(100L);
        testTenant.setIsDeleted(false);

        testWorker = Worker.builder()
                .workerKey("worker-1")
                .user(testUser)
                .tenant(testTenant)
                .build();
        testWorker.setId(10L);
        testWorker.setIsDeleted(false);

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("정상 처리 테스트")
    class SuccessTest {

        @Test
        @DisplayName("X-Tenant-Id 헤더가 있고 User가 Tenant에 속하면 컨텍스트를 설정하고 true를 반환해야 한다")
        void preHandle_WithValidTenantId_SetsContextAndReturnsTrue() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(request.getHeader("X-Tenant-Id")).thenReturn("100");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(tenantContextService.validateTenantAccessOrThrow(1L, 100L))
                    .thenReturn(testWorker);

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            assertThat(TenantContextHolder.getCurrentTenant()).isNotNull();
            assertThat(TenantContextHolder.getCurrentTenant().getId()).isEqualTo(100L);
            assertThat(TenantContextHolder.getCurrentWorker()).isNotNull();
            assertThat(TenantContextHolder.getCurrentWorker().getId()).isEqualTo(10L);
            verify(tenantContextService).validateTenantAccessOrThrow(1L, 100L);
        }
    }

    @Nested
    @DisplayName("스킵 경로 테스트")
    class SkipPathTest {

        @Test
        @DisplayName("/health 경로는 스킵해야 한다")
        void preHandle_WithHealthPath_Skips() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/health");

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            verify(tenantContextService, never()).validateTenantAccessOrThrow(anyLong(), anyLong());
        }

        @Test
        @DisplayName("/auth 경로는 스킵해야 한다")
        void preHandle_WithAuthPath_Skips() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/auth/login");

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            verify(tenantContextService, never()).validateTenantAccessOrThrow(anyLong(), anyLong());
        }

        @Test
        @DisplayName("/swagger 경로는 스킵해야 한다")
        void preHandle_WithSwaggerPath_Skips() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            verify(tenantContextService, never()).validateTenantAccessOrThrow(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("인증 없음 테스트")
    class NoAuthenticationTest {

        @Test
        @DisplayName("인증이 없으면 스킵해야 한다")
        void preHandle_WithoutAuthentication_Skips() throws Exception {
            // Given
            SecurityContextHolder.clearContext();
            when(request.getRequestURI()).thenReturn("/api/vms");

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            verify(tenantContextService, never()).validateTenantAccessOrThrow(anyLong(), anyLong());
        }

        @Test
        @DisplayName("User를 찾을 수 없으면 스킵해야 한다")
        void preHandle_WithUserNotFound_Skips() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

            // When
            boolean result = interceptor.preHandle(request, response, null);

            // Then
            assertThat(result).isTrue();
            verify(tenantContextService, never()).validateTenantAccessOrThrow(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Tenant ID 헤더 테스트")
    class TenantIdHeaderTest {

        @Test
        @DisplayName("X-Tenant-Id 헤더가 없으면 예외를 발생시켜야 한다")
        void preHandle_WithoutTenantIdHeader_ThrowsException() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(request.getHeader("X-Tenant-Id")).thenReturn(null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tenant ID is required");
        }

        @Test
        @DisplayName("X-Tenant-Id 헤더가 빈 문자열이면 예외를 발생시켜야 한다")
        void preHandle_WithEmptyTenantIdHeader_ThrowsException() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(request.getHeader("X-Tenant-Id")).thenReturn("");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tenant ID is required");
        }

        @Test
        @DisplayName("X-Tenant-Id 헤더가 숫자가 아니면 예외를 발생시켜야 한다")
        void preHandle_WithInvalidTenantIdFormat_ThrowsException() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(request.getHeader("X-Tenant-Id")).thenReturn("invalid");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            // When & Then
            assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid tenant ID format");
        }
    }

    @Nested
    @DisplayName("Tenant 접근 검증 실패 테스트")
    class TenantAccessValidationFailureTest {

        @Test
        @DisplayName("User가 Tenant에 속하지 않으면 예외를 발생시켜야 한다")
        void preHandle_WithNoTenantAccess_ThrowsException() throws Exception {
            // Given
            when(request.getRequestURI()).thenReturn("/api/vms");
            when(request.getHeader("X-Tenant-Id")).thenReturn("100");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(tenantContextService.validateTenantAccessOrThrow(1L, 100L))
                    .thenThrow(new BusinessException(com.agenticcp.core.common.enums.CommonErrorCode.FORBIDDEN, "User is not a member of this tenant"));

            // When & Then
            assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("User is not a member of this tenant");
        }
    }

    @Nested
    @DisplayName("afterCompletion 테스트")
    class AfterCompletionTest {

        @Test
        @DisplayName("요청 처리 완료 후 컨텍스트를 정리해야 한다")
        void afterCompletion_ClearsContext() {
            // Given
            TenantContextHolder.setCurrentTenantAndWorker(testTenant, testWorker);

            // When
            interceptor.afterCompletion(request, response, null, null);

            // Then
            assertThat(TenantContextHolder.getCurrentTenant()).isNull();
            assertThat(TenantContextHolder.getCurrentWorker()).isNull();
        }
    }
}

