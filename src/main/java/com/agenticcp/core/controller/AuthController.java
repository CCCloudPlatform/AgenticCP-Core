package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자명과 비밀번호로 로그인하여 JWT 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody LoginRequest loginRequest) {
        
        // 실제 구현에서는 사용자 인증 로직이 들어갑니다
        Map<String, Object> response = new HashMap<>();
        
        // 임시 JWT 토큰 (실제로는 JWT 라이브러리를 사용해야 합니다)
        String mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTY5NDU2MDAwMCwiaWF0IjoxNjk0NTU2NDAwfQ.example_signature";
        
        response.put("accessToken", mockJwtToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600);
        response.put("user", Map.of(
            "username", loginRequest.getUsername(),
            "role", "ADMIN",
            "permissions", new String[]{"READ", "WRITE", "DELETE"}
        ));
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @RequestBody RefreshTokenRequest refreshRequest) {
        
        Map<String, Object> response = new HashMap<>();
        String newJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTY5NDU2MDAwMCwiaWF0IjoxNjk0NTU2NDAwfQ.new_signature";
        
        response.put("accessToken", newJwtToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 3600);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Map<String, Object> user = new HashMap<>();
        user.put("username", "admin");
        user.put("email", "admin@agenticcp.com");
        user.put("role", "ADMIN");
        user.put("permissions", new String[]{"READ", "WRITE", "DELETE"});
        user.put("lastLogin", LocalDateTime.now());
        
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // DTO 클래스들
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}
