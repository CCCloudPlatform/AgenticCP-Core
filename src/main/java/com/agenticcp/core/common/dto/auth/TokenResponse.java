package com.agenticcp.core.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private Long refreshExpiresIn;
}
