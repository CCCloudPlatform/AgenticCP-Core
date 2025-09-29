package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.security.JwtService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.agenticcp.core.common.security.JwtConstants.*;

/**
 * 인증 서비스
 * JWT 기반 로그인, 로그아웃, 토큰 갱신 기능 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final TwoFactorService twoFactorService;
    private final PerformanceMonitoringService performanceMonitoringService;
    private final RetryService retryService;

    /**
     * 사용자 로그인
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        PerformanceMonitoringService.PerformanceMetrics metrics = performanceMonitoringService.measureLoginPerformance(request.getUsername(), () -> {
            log.info("로그인 시도: username={}", request.getUsername());

            // 사용자 조회
            User user = userService.getUserByUsername(request.getUsername())
                    .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_LOGIN_FAILED)
            if (user == null) {
                log.warn("[TODO:BE] 사용자 없음 - AUTH_LOGIN_FAILED");
                return TokenResponse.builder().build();
            }

            // 계정 상태 확인
            validateUserStatus(user);

            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                handleFailedLogin(user);
                // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_LOGIN_FAILED)
                log.warn("[TODO:BE] 비밀번호 불일치 - AUTH_LOGIN_FAILED");
                return TokenResponse.builder().build();
            }

            // 2FA 검증 (활성화된 경우)
            if (user.getTwoFactorEnabled() && request.getTwoFactorCode() != null) {
                validateTwoFactorCode(user, request.getTwoFactorCode());
            } else if (user.getTwoFactorEnabled() && request.getTwoFactorCode() == null) {
                // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_REQUIRED)
                log.warn("[TODO:BE] 2FA 코드 필요 - AUTH_2FA_REQUIRED");
                return TokenResponse.builder().build();
            }

            // 로그인 성공 처리
            handleSuccessfulLogin(user);

            // JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // 리프레시 토큰을 Redis에 저장
            storeRefreshToken(user.getUsername(), refreshToken);

            log.info("로그인 성공: username={}", request.getUsername());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 1시간
                    .user(UserInfoResponse.from(user))
                    .build();
        });
        
        return metrics.getResult();
    }

    /**
     * 리프레시 토큰으로 새로운 액세스 토큰 발급
     */
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        return retryService.retryTokenRefresh("system", () -> {
            String refreshToken = request.getRefreshToken();
            
            PerformanceMonitoringService.PerformanceMetrics metrics = performanceMonitoringService.measureTokenRefreshPerformance("system", () -> {
                log.info("토큰 갱신 요청");

                // 리프레시 토큰 검증
                if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
                    // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_REFRESH_TOKEN_INVALID)
                    log.warn("[TODO:BE] 리프레시 토큰 유효성 실패 - AUTH_REFRESH_TOKEN_INVALID");
                    return TokenResponse.builder().build();
                }

                String username = jwtService.getUsernameFromToken(refreshToken);

                // Redis에서 리프레시 토큰 확인
                String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + username);
                if (!refreshToken.equals(storedToken)) {
                    // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_REFRESH_TOKEN_INVALID)
                    log.warn("[TODO:BE] 저장된 리프레시 토큰과 불일치 - AUTH_REFRESH_TOKEN_INVALID");
                    return TokenResponse.builder().build();
                }

                // 사용자 조회
                User user = userService.getUserByUsername(username)
                        .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (USER_NOT_FOUND)
                if (user == null) {
                    log.warn("[TODO:BE] 사용자 없음 - USER_NOT_FOUND");
                    return TokenResponse.builder().build();
                }

                // 사용자 상태 확인
                validateUserStatus(user);

                // 새로운 토큰 생성
                String newAccessToken = jwtService.generateAccessToken(user);
                String newRefreshToken = jwtService.generateRefreshToken(user);

                // 새로운 리프레시 토큰 저장
                storeRefreshToken(username, newRefreshToken);

                log.info("토큰 갱신 성공: username={}", username);

                return TokenResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .tokenType("Bearer")
                        .expiresIn(3600L)
                        .user(UserInfoResponse.from(user))
                        .build();
            });
            
            return metrics.getResult();
        });
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String username, String accessToken) {
        log.info("로그아웃 요청: username={}", username);

        // Redis에서 리프레시 토큰 제거
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + username);

        // 액세스 토큰을 블랙리스트에 추가 (토큰 만료까지)
        if (accessToken != null && jwtService.validateToken(accessToken)) {
            long expirationTime = jwtService.getTokenExpirationTime(accessToken);
            if (expirationTime > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + accessToken,
                        "blacklisted",
                        Duration.ofSeconds(expirationTime)
                );
            }
        }

        log.info("로그아웃 완료: username={}", username);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * 사용자 상태 검증
     */
    private void validateUserStatus(User user) {
        if (!"ACTIVE".equals(user.getStatus().name())) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_ACCOUNT_DISABLED)
            log.warn("[TODO:BE] 계정 비활성화 - AUTH_ACCOUNT_DISABLED");
            return;
        }

        if (user.isAccountLocked()) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_ACCOUNT_LOCKED)
            log.warn("[TODO:BE] 계정 잠금 - AUTH_ACCOUNT_LOCKED");
            return;
        }
    }

    /**
     * 2FA 코드 검증
     */
    private void validateTwoFactorCode(User user, String twoFactorCode) {
        if (user.getTwoFactorSecret() == null) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_NOT_ENABLED)
            log.warn("[TODO:BE] 2FA 미활성화 - AUTH_2FA_NOT_ENABLED");
            return;
        }

        // 2FA 검증 시도 횟수 확인
        if (twoFactorService.isVerificationAttemptsExceeded(user.getUsername())) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_ATTEMPTS_EXCEEDED)
            log.warn("[TODO:BE] 2FA 시도 횟수 초과 - AUTH_2FA_ATTEMPTS_EXCEEDED");
            return;
        }

        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), twoFactorCode)) {
            twoFactorService.incrementVerificationAttempts(user.getUsername());
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_INVALID)
            log.warn("[TODO:BE] 2FA 코드 불일치 - AUTH_2FA_INVALID");
            return;
        }

        // 검증 성공 시 시도 횟수 초기화
        twoFactorService.resetVerificationAttempts(user.getUsername());
    }

    /**
     * 로그인 실패 처리
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.lockAccount(ACCOUNT_LOCKOUT_MINUTES);
            log.warn("계정 잠금: username={}, attempts={}", user.getUsername(), user.getFailedLoginAttempts());
        }

        userService.updateFailedLoginAttempts(user.getUsername(), user.getFailedLoginAttempts());
        
        if (user.getLockedUntil() != null) {
            userService.lockUserAccount(user.getUsername(), user.getLockedUntil());
        }
    }

    /**
     * 로그인 성공 처리
     */
    private void handleSuccessfulLogin(User user) {
        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        
        userService.updateLastLogin(user.getUsername());
        userService.resetFailedLoginAttempts(user.getUsername());
    }

    /**
     * 리프레시 토큰을 Redis에 저장
     */
    private void storeRefreshToken(String username, String refreshToken) {
        performanceMonitoringService.measureRedisPerformance("store_refresh_token", () -> {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + username,
                    refreshToken,
                    Duration.ofDays(7) // 7일 후 자동 만료
            );
            return null;
        });
    }

    /**
     * 현재 사용자 정보 조회
     */
    public UserInfoResponse getCurrentUser(String username) {
        User user = userService.getUserByUsername(username)
                .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (USER_NOT_FOUND)
        if (user == null) {
            log.warn("[TODO:BE] 현재 사용자 조회 실패 - USER_NOT_FOUND");
            return null;
        }

        return UserInfoResponse.from(user);
    }

    /**
     * 2FA 설정을 위한 QR 코드 URI 생성
     */
    public String generateTwoFactorQRCode(String username) {
        User user = userService.getUserByUsername(username)
                .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (USER_NOT_FOUND)
        if (user == null) {
            log.warn("[TODO:BE] 2FA QR 생성 사용자 없음 - USER_NOT_FOUND");
            return null;
        }

        String secretKey = twoFactorService.generateSecretKey();
        twoFactorService.storeTemporarySecret(username, secretKey);

        return twoFactorService.generateQRCodeUri(user, secretKey);
    }

    /**
     * 2FA 설정 완료
     */
    @Transactional
    public void enableTwoFactor(String username, String verificationCode) {
        User user = userService.getUserByUsername(username)
                .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (USER_NOT_FOUND)
        if (user == null) {
            log.warn("[TODO:BE] 2FA 활성화 사용자 없음 - USER_NOT_FOUND");
            return;
        }

        String secretKey = twoFactorService.getTemporarySecret(username);
        if (secretKey == null) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_SESSION_EXPIRED)
            log.warn("[TODO:BE] 2FA 세션 만료 - AUTH_2FA_SESSION_EXPIRED");
            return;
        }

        if (!twoFactorService.verifyCode(secretKey, verificationCode)) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_INVALID)
            log.warn("[TODO:BE] 2FA 코드 불일치 - AUTH_2FA_INVALID");
            return;
        }

        // 2FA 활성화
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret(secretKey);
        userService.updateUser(user);

        // 임시 시크릿 삭제
        twoFactorService.removeTemporarySecret(username);

        log.info("2FA 활성화 완료: username={}", username);
    }

    /**
     * 2FA 비활성화
     */
    @Transactional
    public void disableTwoFactor(String username, String verificationCode) {
        User user = userService.getUserByUsername(username)
                .orElse(null); // TODO : BE - 비즈니스 예외 처리 보류 (USER_NOT_FOUND)
        if (user == null) {
            log.warn("[TODO:BE] 2FA 비활성화 사용자 없음 - USER_NOT_FOUND");
            return;
        }

        if (!user.getTwoFactorEnabled()) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_NOT_ENABLED)
            log.warn("[TODO:BE] 2FA 미활성화 상태 - AUTH_2FA_NOT_ENABLED");
            return;
        }

        if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), verificationCode)) {
            // TODO : BE - 비즈니스 예외 처리 보류 (AUTH_2FA_INVALID)
            log.warn("[TODO:BE] 2FA 코드 불일치 - AUTH_2FA_INVALID");
            return;
        }

        // 2FA 비활성화
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userService.updateUser(user);

        log.info("2FA 비활성화 완료: username={}", username);
    }
}
