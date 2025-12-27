package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RegisterRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.security.JwtService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.service.TenantService;
import com.agenticcp.core.domain.user.service.UserAuthHistoryService;
import com.agenticcp.core.common.service.TwoFactorService;
import com.agenticcp.core.common.context.TenantContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AuthenticationService 단위 테스트
 * 
 * 인증 서비스의 핵심 기능을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService 단위 테스트")
class AuthenticationServiceTest {

    @Mock
    private UserService userService;
    
    @Mock
    private TenantService tenantService;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private UserAuthHistoryService authHistoryService;
    
    @Mock
    private TwoFactorService twoFactorService;
    
    @Mock
    private TenantContextService tenantContextService;
    
    @Mock
    private HttpServletRequest httpRequest;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userService, tenantContextService, jwtService, passwordEncoder, tenantService, 
                twoFactorService, authHistoryService
        );
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("정상적인 로그인 성공")
        void login_WhenValidCredentials_ShouldReturnTokenResponse() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .build();

            User user = User.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash("encoded_password")
                    .role(UserRole.VIEWER)
                    .status(Status.ACTIVE)
                    .twoFactorEnabled(false)
                    .build();

            when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(user);
            when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn("access_token");
            when(jwtService.generateRefreshToken(user)).thenReturn("refresh_token");

            // When
            TokenResponse result = authenticationService.login(request, httpRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access_token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(result.getTokenType()).isEqualTo("ACCESS");
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void login_WhenInvalidPassword_ShouldThrowBusinessException() {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .username("testuser")
                    .password("wrongpassword")
                    .build();

            User user = User.builder()
                    .username("testuser")
                    .passwordHash("encoded_password")
                    .status(Status.ACTIVE)
                    .build();

            when(userService.getUserByUsernameOrThrow("testuser")).thenReturn(user);
            when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> authenticationService.login(request, httpRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("잘못된 사용자명 또는 비밀번호입니다");
        }
    }
}