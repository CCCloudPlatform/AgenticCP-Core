package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보")
public class UserResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자명", example = "john_doe")
    private String username;

    @Schema(description = "이메일", example = "john.doe@example.com")
    private String email;

    @Schema(description = "이름", example = "John Doe")
    private String name;

    @Schema(description = "사용자 역할", example = "DEVELOPER")
    private UserRole role;

    @Schema(description = "상태", example = "ACTIVE")
    private Status status;

    @Schema(description = "마지막 로그인 시간")
    private LocalDateTime lastLogin;

    @Schema(description = "부서", example = "개발팀")
    private String department;

    @Schema(description = "직책", example = "시니어 개발자")
    private String jobTitle;

    @Schema(description = "전화번호", example = "+82-10-1234-5678")
    private String phoneNumber;

    @Schema(description = "생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시")
    private LocalDateTime updatedAt;
}
