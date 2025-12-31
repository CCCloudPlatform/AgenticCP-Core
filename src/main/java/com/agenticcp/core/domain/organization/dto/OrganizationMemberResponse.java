package com.agenticcp.core.domain.organization.dto;

import com.agenticcp.core.domain.organization.entity.OrganizationMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OrganizationMember 응답 DTO
 * 
 * <p>조직 멤버 정보를 표현하는 응답 DTO입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Schema(description = "조직 멤버 응답")
public class OrganizationMemberResponse {
    
    /** 조직 ID */
    @Schema(description = "조직 ID", example = "1")
    private Long organizationId;
    
    /** 조직명 */
    @Schema(description = "조직명", example = "개발팀")
    private String organizationName;
    
    /** 사용자 ID */
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    /** 사용자명 */
    @Schema(description = "사용자명", example = "john_doe")
    private String username;
    
    /** 사용자 이메일 */
    @Schema(description = "사용자 이메일", example = "john@example.com")
    private String userEmail;
    
    /** 사용자 이름 */
    @Schema(description = "사용자 이름", example = "John Doe")
    private String userName;
    
    /** 조직 내 역할 */
    @Schema(description = "조직 내 역할", example = "ADMIN")
    private String role;
    
    /** 가입일시 */
    @Schema(description = "가입일시", example = "2024-01-01T00:00:00")
    private LocalDateTime joinedAt;
    
    /** 생성일시 */
    @Schema(description = "생성일시", example = "2024-01-01T00:00:00")
    private LocalDateTime createdAt;
    
    /**
     * OrganizationMember 엔티티를 OrganizationMemberResponse로 변환
     * 
     * @param member OrganizationMember 엔티티
     * @return OrganizationMember 응답 DTO
     */
    public static OrganizationMemberResponse from(OrganizationMember member) {
        if (member == null) {
            return null;
        }
        
        return OrganizationMemberResponse.builder()
                .organizationId(member.getOrganization() != null ? member.getOrganization().getId() : null)
                .organizationName(member.getOrganization() != null ? member.getOrganization().getName() : null)
                .userId(member.getUser() != null ? member.getUser().getId() : null)
                .username(member.getUser() != null ? member.getUser().getUsername() : null)
                .userEmail(member.getUser() != null ? member.getUser().getEmail() : null)
                .userName(member.getUser() != null ? member.getUser().getName() : null)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .createdAt(member.getCreatedAt())
                .build();
    }
}

