package com.agenticcp.core.common.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사용자 정보 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private String username;
    private String email;
    private String name;
    private String role;
    private Long tenantId;
    private String tenantKey;
    private List<String> permissions;
    private LocalDateTime lastLogin;
    private Boolean twoFactorEnabled;
}
