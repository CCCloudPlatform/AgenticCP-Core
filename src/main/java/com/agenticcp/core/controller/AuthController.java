package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.dto.auth.LoginRequest;
import com.agenticcp.core.common.dto.auth.RefreshTokenRequest;
import com.agenticcp.core.common.dto.auth.AuthenticationResponse;
import com.agenticcp.core.common.dto.auth.TwoFactorDisableRequest;
import com.agenticcp.core.common.dto.auth.TwoFactorEnableRequest;
import com.agenticcp.core.common.dto.auth.UserInfoResponse;
import com.agenticcp.core.common.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication v1", description = "인증 관련 API v1")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(
        summary = "사용자 로그인", 
        description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급합니다."
    )
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("로그인 API 호출: username={}", request.getUsername());
        
        AuthenticationResponse response = authenticationService.login(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "로그인에 성공했습니다."));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "토큰 갱신", 
        description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다."
    )
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        log.info("토큰 갱신 API 호출");
        
        AuthenticationResponse response = authenticationService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신에 성공했습니다."));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "로그아웃", 
        description = "사용자 로그아웃을 처리하고 리프레시 토큰을 무효화합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication, 
            HttpServletRequest request) {
        
        String username = authentication.getName();
        String accessToken = extractTokenFromRequest(request);
        
        log.info("로그아웃 API 호출: username={}", username);
        
        authenticationService.logout(username, accessToken);
        
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    @GetMapping("/me")
    @Operation(
        summary = "현재 사용자 정보", 
        description = "현재 로그인한 사용자의 정보를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(Authentication authentication) {
        
        String username = authentication.getName();
        
        log.info("현재 사용자 정보 조회 API 호출: username={}", username);
        
        UserInfoResponse response = authenticationService.getCurrentUser(username);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/2fa/qr-code")
    @Operation(
        summary = "2FA QR 코드 생성", 
        description = "2FA 설정을 위한 QR 코드 URI를 생성합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> generateTwoFactorQRCode(Authentication authentication) {
        
        String username = authentication.getName();
        
        log.info("2FA QR 코드 생성 API 호출: username={}", username);
        
        String qrCodeUri = authenticationService.generateTwoFactorQRCode(username);
        
        return ResponseEntity.ok(ApiResponse.success(qrCodeUri, "2FA QR 코드가 생성되었습니다."));
    }

    @PostMapping("/2fa/enable")
    @Operation(
        summary = "2FA 활성화", 
        description = "2FA를 활성화합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> enableTwoFactor(
            Authentication authentication,
            @Valid @RequestBody TwoFactorEnableRequest request) {
        
        String username = authentication.getName();
        
        log.info("2FA 활성화 API 호출: username={}", username);
        
        authenticationService.enableTwoFactor(username, request.getVerificationCode());
        
        return ResponseEntity.ok(ApiResponse.success(null, "2FA가 활성화되었습니다."));
    }

    @PostMapping("/2fa/disable")
    @Operation(
        summary = "2FA 비활성화", 
        description = "2FA를 비활성화합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> disableTwoFactor(
            Authentication authentication,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        
        String username = authentication.getName();
        
        log.info("2FA 비활성화 API 호출: username={}", username);
        
        authenticationService.disableTwoFactor(username, request.getVerificationCode());
        
        return ResponseEntity.ok(ApiResponse.success(null, "2FA가 비활성화되었습니다."));
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}