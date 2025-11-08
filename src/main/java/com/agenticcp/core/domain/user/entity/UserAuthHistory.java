package com.agenticcp.core.domain.user.entity;

import com.agenticcp.core.common.entity.BaseEntity;
import com.agenticcp.core.domain.user.enums.AuthType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 인증 이력 엔티티
 * 회원가입, 로그인, 2FA 인증 등 모든 인증 활동 기록
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Entity
@Table(name = "user_auth_history", indexes = {
    @Index(name = "idx_auth_history_user_id", columnList = "user_id"),
    @Index(name = "idx_auth_history_auth_type", columnList = "auth_type"),
    @Index(name = "idx_auth_history_success", columnList = "is_success"),
    @Index(name = "idx_auth_history_timestamp", columnList = "auth_timestamp"),
    @Index(name = "idx_auth_history_user_type", columnList = "user_id, auth_type"),
    @Index(name = "idx_auth_history_pending", columnList = "is_pending")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthHistory extends BaseEntity {

    /**
     * 사용자 FK
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 인증 종류 (REGISTER, LOGIN, LOGOUT, 2FA_SETUP, 2FA_VERIFY, etc.)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 50)
    private AuthType authType;

    /**
     * 인증 성공 여부
     */
    @NotNull
    @Column(name = "is_success", nullable = false)
    @Builder.Default
    private Boolean isSuccess = false;

    /**
     * 인증 시도 일시
     */
    @NotNull
    @Column(name = "auth_timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime authTimestamp = LocalDateTime.now();

    /**
     * PENDING 여부 (인증 대기 상태)
     * 예: 이메일 인증 대기, 2FA 설정 대기 등
     */
    @Column(name = "is_pending")
    @Builder.Default
    private Boolean isPending = false;

    /**
     * 인증 코드 (2FA TOTP 코드 마스킹, 이메일 인증 코드 등)
     * 보안상 마스킹 처리하여 저장 권장 (예: "******")
     */
    @Column(name = "auth_code_masked", length = 20)
    private String authCodeMasked;

    /**
     * IP 주소
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User Agent (브라우저, 디바이스 정보)
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * 실패 사유 (인증 실패 시)
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * 세션 ID (선택적)
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * JWT 토큰 ID (jti claim 등)
     */
    @Column(name = "token_id", length = 255)
    private String tokenId;

    /**
     * 위치 정보 (선택적, GeoIP 기반)
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * 디바이스 정보 (선택적)
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    /**
     * 추가 메타데이터 (JSON 형식)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 만료 일시 (2FA 인증 코드, 이메일 인증 등)
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // Helper methods
    
    /**
     * 인증이 만료되었는지 확인
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 인증이 PENDING 상태인지 확인
     */
    public boolean isPendingAuth() {
        return isPending != null && isPending && !isExpired();
    }

    /**
     * 성공한 인증인지 확인
     */
    public boolean isSuccessfulAuth() {
        return isSuccess != null && isSuccess;
    }
    
    /**
     * 실패한 인증인지 확인
     */
    public boolean isFailedAuth() {
        return isSuccess != null && !isSuccess;
    }
}

