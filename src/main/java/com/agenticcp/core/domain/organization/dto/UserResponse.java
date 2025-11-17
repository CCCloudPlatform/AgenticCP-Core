package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "사용자 정보")
public class UserResponse {

    /** 사용자 ID */
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    /** 사용자명 */
    @Schema(description = "사용자명", example = "john_doe")
    private String username;

    /** 이메일 */
    @Schema(description = "이메일", example = "john.doe@example.com")
    private String email;

    /** 이름 */
    @Schema(description = "이름", example = "John Doe")
    private String name;

    /** 사용자 역할 */
    @Schema(description = "사용자 역할", example = "DEVELOPER")
    private UserRole role;

    /** 상태 */
    @Schema(description = "상태", example = "ACTIVE")
    private Status status;

    /** 마지막 로그인 시간 */
    @Schema(description = "마지막 로그인 시간")
    private LocalDateTime lastLogin;

    /** 부서 */
    @Schema(description = "부서", example = "개발팀")
    private String department;

    /** 직책 */
    @Schema(description = "직책", example = "시니어 개발자")
    private String jobTitle;

    /** 전화번호 */
    @Schema(description = "전화번호", example = "+82-10-1234-5678")
    private String phoneNumber;

    /** 생성일시 */
    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    /** 수정일시 */
    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;

    /**
     * User 엔티티를 UserResponse로 변환
     * 
     * @param user 사용자 엔티티
     * @return 사용자 응답 DTO
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLogin(user.getLastLogin())
                .department(user.getDepartment())
                .jobTitle(user.getJobTitle())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
