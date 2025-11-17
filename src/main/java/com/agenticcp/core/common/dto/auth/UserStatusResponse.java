package com.agenticcp.core.common.dto.auth;

import com.agenticcp.core.common.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 인증 상태 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponse {
    
    /**
     * 사용자명
     */
    private String username;
    
    /**
     * 사용자 상태
     */
    private Status status;
    
    /**
     * 2FA 활성화 여부
     */
    private boolean twoFactorEnabled;
    
    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLogin;
    
    /**
     * 2FA 설정 필요 여부
     */
    private boolean requiresTwoFactorSetup;
}
