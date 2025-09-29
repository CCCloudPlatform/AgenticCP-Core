package com.agenticcp.core.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 응답 DTO (액세스/리프레시 토큰 포함)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // 만료 시간 (초)
    private UserInfoResponse user;
}
