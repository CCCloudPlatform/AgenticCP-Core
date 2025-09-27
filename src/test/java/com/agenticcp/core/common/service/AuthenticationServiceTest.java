package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.security.JwtService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private PerformanceMonitoringService performanceMonitoringService;
    @Mock
    private RetryService retryService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("testTenant")
                .tenantName("Test Tenant")
                .build();
        ReflectionTestUtils.setField(testTenant, "id", 1L);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .passwordHash("encodedPassword")
                .role(UserRole.SUPER_ADMIN)
                .status(Status.ACTIVE)
                .tenant(testTenant)
                .twoFactorEnabled(false)
                .failedLoginAttempts(0)
                .lockedUntil(null)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {

        @Test
        @DisplayName("성공적인 로그인")
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password");

            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtService.generateAccessToken(testUser)).thenReturn("accessToken");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(performanceMonitoringService.measureLoginPerformance(anyString(), any()))
                    .thenAnswer(invocation -> {
                        PerformanceMonitoringService.PerformanceMetrics metrics = new PerformanceMonitoringService.PerformanceMetrics("LOGIN", "testuser", 100L, true);
                        metrics.setResult(TokenResponse.builder()
                                .accessToken("accessToken")
                                .refreshToken("refreshToken")
                                .tokenType("Bearer")
                                .expiresIn(3600L)
                                .user(UserInfoResponse.from(testUser))
                                .build());
                        return metrics;
                    });

            TokenResponse response = authenticationService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
            assertThat(response.getUser().getUsername()).isEqualTo("testuser");

            verify(userService, times(1)).updateLastLogin("testuser");
            verify(valueOperations, times(1)).set(eq("refresh_token:testuser"), eq("refreshToken"), any(Duration.class));
        }

        @Test
        @DisplayName("비활성화된 계정으로 로그인 시도 시 BusinessException 발생")
        void login_InactiveUser_ThrowsException() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password");

            testUser.setStatus(Status.INACTIVE);
            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(performanceMonitoringService.measureLoginPerformance(anyString(), any()))
                    .thenThrow(new BusinessException("계정이 비활성화되었습니다"));

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("계정이 비활성화되었습니다");

            verify(userService, never()).updateLastLogin(anyString());
        }

        @Test
        @DisplayName("잠금된 계정으로 로그인 시도 시 BusinessException 발생")
        void login_LockedUser_ThrowsException() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password");

            testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(performanceMonitoringService.measureLoginPerformance(anyString(), any()))
                    .thenThrow(new BusinessException("계정이 잠금되었습니다"));

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("계정이 잠금되었습니다");

            verify(userService, never()).updateLastLogin(anyString());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시도 시 BusinessException 발생 및 실패 횟수 증가")
        void login_WrongPassword_ThrowsExceptionAndIncrementsFailedAttempts() {
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);
            when(performanceMonitoringService.measureLoginPerformance(anyString(), any()))
                    .thenThrow(new BusinessException("잘못된 사용자명 또는 비밀번호입니다"));

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("잘못된 사용자명 또는 비밀번호입니다");

            verify(userService, times(1)).handleFailedLogin("testuser");
            verify(userService, never()).updateLastLogin(anyString());
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 로그인 시도 시 ResourceNotFoundException 발생")
        void login_UserNotFound_ThrowsException() {
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistent");
            request.setPassword("password");

            when(userService.getUserByUsername("nonexistent")).thenReturn(java.util.Optional.empty());
            when(performanceMonitoringService.measureLoginPerformance(anyString(), any()))
                    .thenThrow(new BusinessException("잘못된 사용자명 또는 비밀번호입니다"));

            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(BusinessException.class);

            verify(userService, never()).handleFailedLogin(anyString());
            verify(userService, never()).updateLastLogin(anyString());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 테스트")
    class RefreshTokenTest {

        @Test
        @DisplayName("성공적인 토큰 갱신")
        void refreshToken_Success() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("oldRefreshToken");

            when(jwtService.validateToken("oldRefreshToken")).thenReturn(true);
            when(jwtService.isRefreshToken("oldRefreshToken")).thenReturn(true);
            when(jwtService.getUsernameFromToken("oldRefreshToken")).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(valueOperations.get("refresh_token:testuser")).thenReturn("oldRefreshToken");
            when(jwtService.generateAccessToken(testUser)).thenReturn("newAccessToken");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("newRefreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(3600000L);
            when(jwtService.getRefreshTokenExpiration()).thenReturn(604800000L);
            when(retryService.retryTokenRefresh(anyString(), any())).thenReturn(
                    TokenResponse.builder()
                            .accessToken("newAccessToken")
                            .refreshToken("newRefreshToken")
                            .tokenType("Bearer")
                            .expiresIn(3600L)
                            .build()
            );

            TokenResponse response = authenticationService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시도 시 BusinessException 발생")
        void refreshToken_InvalidToken_ThrowsException() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalidToken");

            when(jwtService.validateToken("invalidToken")).thenReturn(false);
            when(retryService.retryTokenRefresh(anyString(), any()))
                    .thenThrow(new BusinessException("유효하지 않은 리프레시 토큰입니다"));

            assertThatThrownBy(() -> authenticationService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 리프레시 토큰입니다");

            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("Redis에 없는 리프레시 토큰으로 갱신 시도 시 BusinessException 발생")
        void refreshToken_TokenNotInRedis_ThrowsException() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("validButNotInRedisToken");

            when(jwtService.validateToken("validButNotInRedisToken")).thenReturn(true);
            when(jwtService.isRefreshToken("validButNotInRedisToken")).thenReturn(true);
            when(jwtService.getUsernameFromToken("validButNotInRedisToken")).thenReturn("testuser");
            when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));
            when(valueOperations.get("refresh_token:testuser")).thenReturn(null); // Not in Redis
            when(retryService.retryTokenRefresh(anyString(), any()))
                    .thenThrow(new BusinessException("유효하지 않은 리프레시 토큰입니다"));

            assertThatThrownBy(() -> authenticationService.refreshToken(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 리프레시 토큰입니다");

            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {

        @Test
        @DisplayName("성공적인 로그아웃")
        void logout_Success() {
            String username = "testuser";
            String accessToken = "someAccessToken";

            when(jwtService.validateToken(accessToken)).thenReturn(true);
            when(jwtService.getTokenExpirationTime(accessToken)).thenReturn(100L); // 100 seconds left

            authenticationService.logout(username, accessToken);

            verify(redisTemplate, times(1)).delete("refresh_token:" + username);
            verify(valueOperations, times(1)).set(eq("blacklist:" + accessToken), eq("blacklisted"), any(Duration.class));
        }

        @Test
        @DisplayName("액세스 토큰이 유효하지 않아도 로그아웃 성공 (리프레시 토큰만 제거)")
        void logout_InvalidAccessToken_Success() {
            String username = "testuser";
            String accessToken = "invalidAccessToken";

            when(jwtService.validateToken(accessToken)).thenReturn(false);

            authenticationService.logout(username, accessToken);

            verify(redisTemplate, times(1)).delete("refresh_token:" + username);
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Test
    @DisplayName("현재 사용자 정보 조회")
    void getCurrentUser_Success() {
        when(userService.getUserByUsername("testuser")).thenReturn(java.util.Optional.of(testUser));

        UserInfoResponse response = authenticationService.getCurrentUser("testuser");

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }
}
