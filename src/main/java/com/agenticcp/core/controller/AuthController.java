package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.TokenResponse;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.service.AuthenticationService;
import com.agenticcp.core.common.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 REST API 컨트롤러
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("[AuthController] login - username={}", loginRequest.getUsername());
        
        try {
            TokenResponse tokenResponse = authenticationService.login(loginRequest);
            
            return ResponseEntity.ok(ApiResponse.success(tokenResponse, "로그인에 성공했습니다."));
            
        } catch (Exception e) {
            log.error("[AuthController] login - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.LOGIN_FAILED, e.getMessage()));
        }
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        log.info("[AuthController] refreshToken");
        
        try {
            TokenResponse tokenResponse = authenticationService.refreshToken(refreshTokenRequest);
            
            return ResponseEntity.ok(ApiResponse.success(tokenResponse, "토큰 갱신에 성공했습니다."));
            
        } catch (Exception e) {
            log.error("[AuthController] refreshToken - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.TOKEN_REFRESH_FAILED, e.getMessage()));
        }
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "사용자 로그아웃", description = "현재 사용자를 로그아웃하고 토큰을 무효화합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        log.info("[AuthController] logout");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = (Authentication) request.getUserPrincipal();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                
                // 액세스 토큰을 블랙리스트에 추가
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    authenticationService.blacklistToken(token);
                }
                
                // 로그아웃 처리
                authenticationService.logout(username);
            }
            
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃에 성공했습니다."));
            
        } catch (Exception e) {
            log.error("[AuthController] logout - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.LOGOUT_FAILED, e.getMessage()));
        }
    }

    /**
     * 현재 사용자 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(HttpServletRequest request) {
        log.info("[AuthController] getCurrentUser");
        
        try {
            // 현재 인증된 사용자 정보 가져오기
            Authentication authentication = (Authentication) request.getUserPrincipal();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error(AuthErrorCode.UNAUTHORIZED));
            }
            
            String username = authentication.getName();
            UserInfoResponse userInfo = authenticationService.getCurrentUser(username);
            
            return ResponseEntity.ok(ApiResponse.success(userInfo, "사용자 정보 조회에 성공했습니다."));
            
        } catch (Exception e) {
            log.error("[AuthController] getCurrentUser - error", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AuthErrorCode.USER_INFO_FAILED, e.getMessage()));
        }
    }

    /**
     * 토큰 검증
     */
    @GetMapping("/validate")
    @Operation(summary = "토큰 검증", description = "현재 토큰의 유효성을 검증합니다.")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(HttpServletRequest request) {
        log.info("[AuthController] validateToken");
        
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResponse.success(false, "토큰이 없습니다."));
            }
            
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            boolean isValid = jwtService.isTokenValid(token, username);
            
            return ResponseEntity.ok(ApiResponse.success(isValid, 
                    isValid ? "토큰이 유효합니다." : "토큰이 유효하지 않습니다."));
            
        } catch (Exception e) {
            log.error("[AuthController] validateToken - error", e);
            return ResponseEntity.ok(ApiResponse.success(false, "토큰 검증에 실패했습니다."));
        }
    }
}