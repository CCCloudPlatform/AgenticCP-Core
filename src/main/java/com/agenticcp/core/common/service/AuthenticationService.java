package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.RegisterRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.security.JwtService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.user.service.UserService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.agenticcp.core.common.security.JwtConstants.TOKEN_TYPE_ACCESS;

/**
 * 인증 서비스
 * JWT 기반 로그인, 토큰 갱신, 로그아웃 기능 제공
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthenticationService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;
    private final TwoFactorService twoFactorService;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 사용자 회원가입 처리
     * @param request 회원가입 요청 DTO
     * @return 생성된 사용자 정보와 JWT 토큰
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        log.info("[AuthenticationService] register - username={}", request.getUsername());

        // 1. 사용자명 중복 체크
        if (userService.existsByUsername(request.getUsername())) {
            log.warn("[AuthenticationService] register - Username already exists: {}", request.getUsername());
            throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 2. 이메일 중복 체크
        if (userService.existsByEmail(request.getEmail())) {
            log.warn("[AuthenticationService] register - Email already exists: {}", request.getEmail());
            throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. 테넌트 유효성 검사 (선택적)
        Tenant tenant = null;
        if (request.getTenantKey() != null && !request.getTenantKey().isEmpty()) {
            tenant = tenantService.getTenantByKey(request.getTenantKey())
                    .orElseThrow(() -> {
                        log.warn("[AuthenticationService] register - Invalid tenant key: {}", request.getTenantKey());
                        return new BusinessException(AuthErrorCode.INVALID_TENANT_KEY);
                    });
        }

        // 4. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. 사용자 생성
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(encodedPassword)
                .name(request.getName())
                .role(UserRole.VIEWER) // 기본 역할 부여
                .status(Status.ACTIVE) // 기본 상태 활성
                .tenant(tenant)
                .build();

        User savedUser = userService.saveUser(newUser);
        log.info("[AuthenticationService] register - User registered successfully: {}", savedUser.getUsername());

        // 6. JWT 토큰 생성 및 반환 (회원가입 즉시 로그인 처리)
        return generateTokens(savedUser.getUsername());
    }

    /**
     * 사용자 로그인
     */
    public TokenResponse login(LoginRequest loginRequest) {
        log.info("[AuthenticationService] login - username={}", loginRequest.getUsername());
        
        try {
            // 사용자 조회
            User user = userService.getUserByUsernameOrThrow(loginRequest.getUsername());
            
            // 계정 상태 확인
            if (user.isAccountLocked()) {
                log.warn("[AuthenticationService] login - account locked username={}", loginRequest.getUsername());
                throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
            }
            
            // PENDING 상태 사용자 체크 (2FA 설정 필요)
            if (user.getStatus() == com.agenticcp.core.common.enums.Status.PENDING) {
                log.warn("[AuthenticationService] login - pending user (2FA setup required) username={}", loginRequest.getUsername());
                throw new BusinessException(AuthErrorCode.ACCOUNT_PENDING_2FA_SETUP);
            }
            
            if (user.getStatus() != com.agenticcp.core.common.enums.Status.ACTIVE) {
                log.warn("[AuthenticationService] login - inactive account username={}", loginRequest.getUsername());
                throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
            
            // 비밀번호 확인
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                log.warn("[AuthenticationService] login - invalid password username={}", loginRequest.getUsername());
                userService.handleFailedLogin(loginRequest.getUsername());
                throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            
            // 2FA 활성화된 경우 TOTP 코드 검증
            if (user.isTwoFactorEnabled()) {
                if (loginRequest.getTotpCode() == null || loginRequest.getTotpCode().trim().isEmpty()) {
                    log.warn("[AuthenticationService] login - 2FA enabled but no TOTP code provided username={}", loginRequest.getUsername());
                    throw new BusinessException(AuthErrorCode.TOTP_CODE_REQUIRED);
                }
                
                if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), loginRequest.getTotpCode())) {
                    log.warn("[AuthenticationService] login - invalid TOTP code username={}", loginRequest.getUsername());
                    userService.handleFailedLogin(loginRequest.getUsername());
                    throw new BusinessException(AuthErrorCode.INVALID_TOTP_CODE);
                }
                
                log.debug("[AuthenticationService] login - TOTP code verified username={}", loginRequest.getUsername());
            }
            
            // 로그인 성공 처리
            userService.updateLastLogin(loginRequest.getUsername());
            
            // 토큰 생성
            TokenResponse tokenResponse = generateTokens(user.getUsername());
            
            log.info("[AuthenticationService] login - success username={}", loginRequest.getUsername());
            
            return tokenResponse;
                    
        } catch (ResourceNotFoundException e) {
            log.warn("[AuthenticationService] login - user not found username={}", loginRequest.getUsername());
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 토큰 갱신
     */
    public TokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        log.info("[AuthenticationService] refreshToken");
        
        try {
            // 리프레시 토큰 검증
            if (!jwtService.isTokenValid(refreshTokenRequest.getRefreshToken(), 
                    jwtService.extractUsername(refreshTokenRequest.getRefreshToken()))) {
                log.warn("[AuthenticationService] refreshToken - invalid token");
                throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
            }
            
            String username = jwtService.extractUsername(refreshTokenRequest.getRefreshToken());
            
            // Redis에서 리프레시 토큰 확인
            String refreshTokenKey = "refresh_token:" + username;
            String storedRefreshToken = null;
            if (redisTemplate != null) {
                storedRefreshToken = (String) redisTemplate.opsForValue().get(refreshTokenKey);
            }
            
            if (storedRefreshToken == null) {
                log.warn("[AuthenticationService] refreshToken - no stored token in Redis (allowed when Redis disabled)");
            }
            if (storedRefreshToken != null && !storedRefreshToken.equals(refreshTokenRequest.getRefreshToken())) {
                log.warn("[AuthenticationService] refreshToken - token mismatch username={}", username);
                throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISMATCH);
            }
            
            // 사용자 조회
            User user = userService.getUserByUsernameOrThrow(username);
            
            // 새 토큰 생성
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            
            // 새 리프레시 토큰을 Redis에 저장
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(refreshTokenKey, newRefreshToken, 7, TimeUnit.DAYS);
            }
            
            log.info("[AuthenticationService] refreshToken - success username={}", username);
            
            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType(TOKEN_TYPE_ACCESS)
                    .expiresIn(3600L) // 1시간
                    .refreshExpiresIn(604800L) // 7일
                    .build();
                    
        } catch (Exception e) {
            log.error("[AuthenticationService] refreshToken - error", e);
            throw new BusinessException(AuthErrorCode.TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * 로그아웃
     */
    public void logout(String username) {
        log.info("[AuthenticationService] logout - username={}", username);
        
        try {
            // Redis에서 리프레시 토큰 삭제
            String refreshTokenKey = "refresh_token:" + username;
            if (redisTemplate != null) {
                redisTemplate.delete(refreshTokenKey);
            }
            
            log.info("[AuthenticationService] logout - success username={}", username);
            
        } catch (Exception e) {
            log.error("[AuthenticationService] logout - error", e);
            throw new BusinessException(AuthErrorCode.LOGOUT_FAILED);
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String username) {
        log.info("[AuthenticationService] getCurrentUser - username={}", username);
        
        try {
            User user = userService.getUserByUsernameOrThrow(username);
            
            // 권한 목록 추출 (임시로 빈 리스트)
            List<String> permissions = List.of();
            
            return UserInfoResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                    .tenantKey(user.getTenant() != null ? user.getTenant().getTenantKey() : null)
                    .permissions(permissions)
                    .lastLogin(user.getLastLogin())
                    .twoFactorEnabled(user.getTwoFactorEnabled())
                    .build();
                    
        } catch (Exception e) {
            log.error("[AuthenticationService] getCurrentUser - error", e);
            throw new BusinessException(AuthErrorCode.USER_INFO_FAILED);
        }
    }

    /**
     * 토큰을 블랙리스트에 추가
     */
    public void blacklistToken(String token) {
        log.info("[AuthenticationService] blacklistToken");
        
        try {
            String blacklistKey = "blacklist:" + token;
            // 토큰 만료 시간까지 블랙리스트에 저장
            long expirationTime = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (redisTemplate != null && expirationTime > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", expirationTime, TimeUnit.MILLISECONDS);
            }
            
            log.info("[AuthenticationService] blacklistToken - success");
            
        } catch (Exception e) {
            log.error("[AuthenticationService] blacklistToken - error", e);
        }
    }

    /**
     * JWT 토큰 생성 (공통 메서드)
     */
    private TokenResponse generateTokens(String username) {
        User user = userService.getUserByUsernameOrThrow(username);
        
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // 리프레시 토큰을 Redis에 저장 (7일)
        String refreshTokenKey = "refresh_token:" + username;
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(refreshTokenKey, refreshToken, 7, TimeUnit.DAYS);
        } else {
            log.debug("[AuthenticationService] RedisTemplate not configured. Skipping refresh token store.");
        }
        
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_ACCESS)
                .expiresIn(3600L) // 1시간
                .refreshExpiresIn(604800L) // 7일
                .build();
    }
}
