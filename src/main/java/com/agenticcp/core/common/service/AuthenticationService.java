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
import com.agenticcp.core.domain.user.enums.AuthType;
import com.agenticcp.core.domain.user.service.UserAuthHistoryService;
import jakarta.servlet.http.HttpServletRequest;
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
 * @since 2025-10-24
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
    private final UserAuthHistoryService authHistoryService;
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 사용자 회원가입 처리
     * @param request 회원가입 요청 DTO
     * @param httpRequest HTTP 요청 (IP 주소, User-Agent 추출용)
     * @return 생성된 사용자 정보와 JWT 토큰
     */
    @Transactional
    public TokenResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("[AuthenticationService] register - username={}", request.getUsername());

        User savedUser = null;
        try {
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
            // 2FA 정책에 따라 초기 상태 결정
            // TODO: 설정에서 2FA 필수 여부 확인 (SecurityPolicyService 또는 PlatformConfig에서 가져오기)
            boolean twoFactorRequired = true; // 기본값: 2FA 필수
            Status initialStatus = twoFactorRequired ? Status.PENDING : Status.ACTIVE;
            
            // 설계 B: User는 전역 계정이므로 tenant 필드 제거
            User newUser = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(encodedPassword)
                    .name(request.getName())
                    .role(UserRole.VIEWER) // 기본 역할 부여
                    .status(initialStatus) // 2FA 정책에 따라 PENDING 또는 ACTIVE
                    .build();
            
            // TODO: 설계 B - 테넌트가 제공된 경우 Worker를 생성해야 함
            // if (tenant != null) {
            //     workerService.createWorker(newUser.getId(), tenant.getId());
            // }

            savedUser = userService.saveUser(newUser);
            log.info("[AuthenticationService] register - User registered successfully: {}", savedUser.getUsername());

            // 6. 회원가입 이력 저장
            if (httpRequest != null) {
                authHistoryService.recordAuthHistoryFromRequest(
                        savedUser, AuthType.REGISTER, true, httpRequest, null);
            }

            // 7. JWT 토큰 생성 및 반환 (회원가입 즉시 로그인 처리)
            return generateTokens(savedUser.getUsername());
            
        } catch (BusinessException e) {
            // 회원가입 실패 이력 저장 (사용자 생성 전이므로 username으로만 기록)
            log.error("[AuthenticationService] register failed - username={}, reason={}", 
                    request.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * 사용자 로그인
     */
    public TokenResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) {
        log.info("[AuthenticationService] login - username={}", loginRequest.getUsername());
        
        User user = null;
        String failureReason = null;
        boolean loginSuccess = false;
        
        try {
            // 사용자 조회
            user = userService.getUserByUsernameOrThrow(loginRequest.getUsername());
            
            // 계정 상태 확인
            if (user.isAccountLocked()) {
                failureReason = "계정이 잠겨있습니다";
                log.warn("[AuthenticationService] login - account locked username={}", loginRequest.getUsername());
                // 계정 잠금 실패 이력 저장
                if (httpRequest != null) {
                    authHistoryService.recordAuthHistoryFromRequest(
                            user, AuthType.LOGIN, false, httpRequest, failureReason);
                }
                throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
            }
            
            // 계정 상태 확인 (PENDING과 ACTIVE만 로그인 허용)
            if (user.getStatus() == com.agenticcp.core.common.enums.Status.PENDING) {
                log.info("[AuthenticationService] login - PENDING user (2FA setup required) username={}", loginRequest.getUsername());
                // 2FA 설정을 위해 제한적 로그인 허용
            } else if (user.getStatus() != com.agenticcp.core.common.enums.Status.ACTIVE) {
                failureReason = "비활성 계정입니다";
                log.warn("[AuthenticationService] login - inactive account status={} username={}", 
                    user.getStatus(), loginRequest.getUsername());
                // 비활성 계정 실패 이력 저장
                if (httpRequest != null) {
                    authHistoryService.recordAuthHistoryFromRequest(
                            user, AuthType.LOGIN, false, httpRequest, failureReason);
                }
                throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
            
            // 비밀번호 확인
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                failureReason = "비밀번호가 일치하지 않습니다";
                log.warn("[AuthenticationService] login - invalid password username={}", loginRequest.getUsername());
                userService.handleFailedLogin(loginRequest.getUsername());
                // 로그인 실패 이력 저장
                if (httpRequest != null) {
                    authHistoryService.recordAuthHistoryFromRequest(
                            user, AuthType.LOGIN, false, httpRequest, failureReason);
                }
                throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
            }
            
            // 2FA 활성화된 경우 TOTP 코드 검증
            if (user.isTwoFactorEnabled()) {
                if (loginRequest.getTotpCode() == null || loginRequest.getTotpCode().trim().isEmpty()) {
                    failureReason = "2FA 코드가 필요합니다";
                    log.warn("[AuthenticationService] login - 2FA enabled but no TOTP code provided username={}", loginRequest.getUsername());
                    // 2FA 검증 실패 이력 저장
                    if (httpRequest != null) {
                        authHistoryService.recordAuthHistoryFromRequest(
                                user, AuthType.TWO_FACTOR_VERIFY, false, httpRequest, failureReason);
                    }
                    throw new BusinessException(AuthErrorCode.TOTP_CODE_REQUIRED);
                }
                
                if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), loginRequest.getTotpCode())) {
                    failureReason = "2FA 코드가 유효하지 않습니다";
                    log.warn("[AuthenticationService] login - invalid TOTP code username={}", loginRequest.getUsername());
                    userService.handleFailedLogin(loginRequest.getUsername());
                    // 2FA 검증 실패 이력 저장
                    if (httpRequest != null) {
                        String maskedCode = "******";
                        authHistoryService.recordAuthHistoryFromRequest(
                                user, AuthType.TWO_FACTOR_VERIFY, false, httpRequest, failureReason);
                    }
                    throw new BusinessException(AuthErrorCode.INVALID_TOTP_CODE);
                }
                
                log.debug("[AuthenticationService] login - TOTP code verified username={}", loginRequest.getUsername());
                // 2FA 검증 성공 이력 저장
                if (httpRequest != null) {
                    authHistoryService.recordAuthHistoryFromRequest(
                            user, AuthType.TWO_FACTOR_VERIFY, true, httpRequest, null);
                }
            }
            
            // 로그인 성공 처리
            userService.updateLastLogin(loginRequest.getUsername());
            loginSuccess = true;
            
            // 로그인 성공 이력 저장
            if (httpRequest != null) {
                authHistoryService.recordAuthHistoryFromRequest(
                        user, AuthType.LOGIN, true, httpRequest, null);
            }
            
            // 토큰 생성
            TokenResponse tokenResponse = generateTokens(user.getUsername());
            
            log.info("[AuthenticationService] login - success username={}", loginRequest.getUsername());
            
            return tokenResponse;
                    
        } catch (ResourceNotFoundException e) {
            failureReason = "사용자를 찾을 수 없습니다";
            log.warn("[AuthenticationService] login - user not found username={}", loginRequest.getUsername());
            // 사용자 없음 실패 이력 저장 불가 (UserAuthHistory 엔티티에서 user가 필수이므로)
            // 보안상 사용자명을 노출하지 않기 위해 이력 저장 생략
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        } catch (BusinessException e) {
            // 이미 이력이 저장된 경우는 스킵
            throw e;
        }
    }

    /**
     * 토큰 갱신
     * 리프레시 토큰을 검증하고 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.
     * 
     * @param refreshTokenRequest 리프레시 토큰 요청 정보
     * @return 새로운 액세스 토큰과 리프레시 토큰
     * @throws BusinessException 리프레시 토큰이 유효하지 않거나, 사용자 상태가 ACTIVE가 아니거나, 계정이 잠긴 경우
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
            
            // 사용자 상태 검증
            if (user.getStatus() != Status.ACTIVE) {
                if (user.getStatus() == Status.PENDING) {
                    // 2FA 설정 중인 경우 제한적 허용 검토
                    log.warn("[AuthenticationService] refreshToken - PENDING user attempted token refresh: {}", username);
                } else {
                    log.warn("[AuthenticationService] refreshToken - inactive user attempted token refresh: {} status={}", 
                        username, user.getStatus());
                }
                throw new BusinessException(AuthErrorCode.ACCOUNT_INACTIVE);
            }
            
            // 계정 잠금 확인
            if (user.isAccountLocked()) {
                log.warn("[AuthenticationService] refreshToken - locked account attempted token refresh: {}", username);
                throw new BusinessException(AuthErrorCode.ACCOUNT_LOCKED);
            }
            
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
            
            // 설계 B: User는 전역 계정이므로 tenant 정보는 Worker를 통해 가져와야 함
            // TODO: 현재 활성 테넌트 컨텍스트에서 가져오거나 Worker 목록에서 선택
            return UserInfoResponse.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .tenantId(null) // TODO: Worker를 통해 현재 테넌트 정보 가져오기
                    .tenantKey(null) // TODO: Worker를 통해 현재 테넌트 정보 가져오기
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
     * 
     * <p>로그아웃된 토큰을 블랙리스트에 추가하여 재사용을 방지합니다.
     * 토큰의 만료 시간까지 블랙리스트에 유지됩니다.</p>
     * 
     * <p>Redis 비활성화 시 대안:</p>
     * <ul>
     *   <li>데이터베이스에 블랙리스트 테이블 생성하여 관리</li>
     *   <li>인메모리 캐시(Caffeine, Guava Cache) 사용</li>
     *   <li>JWT 토큰에 버전 번호 추가하여 강제 무효화</li>
     * </ul>
     * 
     * <p>현재 구현: Redis가 없으면 블랙리스트 기능이 작동하지 않음
     * (로그아웃한 토큰도 만료 전까지 유효하게 됨)</p>
     * 
     * @param token 블랙리스트에 추가할 JWT 토큰
     */
    public void blacklistToken(String token) {
        log.info("[AuthenticationService] blacklistToken");
        
        try {
            String blacklistKey = "blacklist:" + token;
            // 토큰 만료 시간까지 블랙리스트에 저장
            long expirationTime = jwtService.extractExpiration(token).getTime() - System.currentTimeMillis();
            if (redisTemplate != null && expirationTime > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", expirationTime, TimeUnit.MILLISECONDS);
                log.info("[AuthenticationService] blacklistToken - success");
            } else {
                log.warn("[AuthenticationService] blacklistToken - Redis not available, token blacklist not applied");
            }
            
        } catch (Exception e) {
            log.error("[AuthenticationService] blacklistToken - error", e);
        }
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인
     * 
     * <p>Redis 비활성화 시:
     * 블랙리스트 확인이 불가능하므로 false 반환합니다.
     * 보안상 Redis 사용을 권장하거나 대안 구현이 필요합니다.</p>
     * 
     * @param token 확인할 JWT 토큰
     * @return 블랙리스트에 있으면 true, 없거나 Redis가 비활성화된 경우 false
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            if (redisTemplate == null) {
                log.debug("[AuthenticationService] RedisTemplate is not available. Token blacklist check skipped.");
                return false;
            }
            String blacklistKey = "blacklist:" + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("[AuthenticationService] Failed to check token blacklist", e);
            return false;
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
