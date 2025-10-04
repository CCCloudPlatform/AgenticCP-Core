package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.RegisterRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.common.security.JwtService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.service.TenantService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final TenantService tenantService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

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
            
            // 로그인 성공 처리
            userService.updateLastLogin(loginRequest.getUsername());
            
            // 토큰 생성
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            
            // 리프레시 토큰을 Redis에 저장 (7일)
            String refreshTokenKey = "refresh_token:" + user.getUsername();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(refreshTokenKey, refreshToken, 7, TimeUnit.DAYS);
            } else {
                log.debug("[AuthenticationService] RedisTemplate not configured. Skipping refresh token store.");
            }
            
            log.info("[AuthenticationService] login - success username={}", loginRequest.getUsername());
            
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType(TOKEN_TYPE_ACCESS)
                    .expiresIn(3600L) // 1시간
                    .refreshExpiresIn(604800L) // 7일
                    .build();
                    
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
     * 사용자 회원가입
     */
    public TokenResponse register(RegisterRequest registerRequest) {
        log.info("[AuthenticationService] register - username={} email={}", 
                registerRequest.getUsername(), 
                registerRequest.getEmail());
        
        try {
            // 1. 사용자명 중복 체크
            if (userService.getUserByUsername(registerRequest.getUsername()).isPresent()) {
                log.warn("[AuthenticationService] register - username already exists username={}", 
                        registerRequest.getUsername());
                throw new BusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }
            
            // 2. 이메일 중복 체크
            if (userService.getUserByEmail(registerRequest.getEmail()).isPresent()) {
                log.warn("[AuthenticationService] register - email already exists email={}", 
                        registerRequest.getEmail());
                throw new BusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
            }
            
            // 3. 테넌트 조회 (있는 경우)
            Tenant tenant = null;
            if (StringUtils.hasText(registerRequest.getTenantKey())) {
                tenant = tenantService.getTenantByKey(registerRequest.getTenantKey())
                        .orElseThrow(() -> {
                            log.warn("[AuthenticationService] register - invalid tenant key tenantKey={}", 
                                    registerRequest.getTenantKey());
                            return new BusinessException(AuthErrorCode.INVALID_TENANT_KEY);
                        });
                
                // 테넌트가 활성 상태인지 확인
                if (tenant.getStatus() != Status.ACTIVE) {
                    log.warn("[AuthenticationService] register - inactive tenant tenantKey={}", 
                            registerRequest.getTenantKey());
                    throw new BusinessException(AuthErrorCode.INVALID_TENANT_KEY, "비활성화된 테넌트입니다.");
                }
            }
            
            // 4. User 엔티티 생성
            User newUser = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .name(registerRequest.getName())
                    .passwordHash(registerRequest.getPassword()) // UserService.createUser에서 해싱됨
                    .tenant(tenant)
                    .role(UserRole.VIEWER) // 기본 역할
                    .status(Status.ACTIVE) // 활성 상태
                    .twoFactorEnabled(false)
                    .failedLoginAttempts(0)
                    .timezone("UTC")
                    .language("ko")
                    .build();
            
            // 5. 사용자 저장 (비밀번호 자동 해싱)
            User savedUser = userService.createUser(newUser);
            
            // 6. JWT 토큰 생성
            String accessToken = jwtService.generateAccessToken(savedUser);
            String refreshToken = jwtService.generateRefreshToken(savedUser);
            
            // 7. 리프레시 토큰을 Redis에 저장
            String refreshTokenKey = "refresh_token:" + savedUser.getUsername();
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(refreshTokenKey, refreshToken, 7, TimeUnit.DAYS);
            }
            
            log.info("[AuthenticationService] register - success username={}", savedUser.getUsername());
            
            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType(TOKEN_TYPE_ACCESS)
                    .expiresIn(3600L) // 1시간
                    .refreshExpiresIn(604800L) // 7일
                    .build();
                    
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AuthenticationService] register - error", e);
            throw new BusinessException(AuthErrorCode.REGISTRATION_FAILED, e.getMessage());
        }
    }
}
