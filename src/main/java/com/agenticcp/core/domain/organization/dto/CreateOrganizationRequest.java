package com.agenticcp.core.domain.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 조직 생성 요청 DTO
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
@Schema(description = "조직 생성 요청")
public class CreateOrganizationRequest {
    
    /** 조직명 */
    @NotBlank(message = "조직명은 필수입니다")
    @Size(max = 255, message = "조직명은 255자를 초과할 수 없습니다")
    @Schema(description = "조직명", example = "개발팀", required = true)
    private String orgName;
    
    /** 조직 설명 */
    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다")
    @Schema(description = "조직 설명", example = "개발 관련 업무를 담당하는 조직")
    private String description;
    
    /** 상위 조직 ID */
    @Positive(message = "상위 조직 ID는 양수여야 합니다")
    @Schema(description = "상위 조직 ID", example = "1")
    private Long parentOrganizationId;
    
    /** 조직 타입 */
    @Schema(description = "조직 타입", example = "DEPARTMENT")
    private String orgType;
    
    /** 연락처 이메일 */
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "연락처 이메일", example = "contact@example.com")
    private String contactEmail;
    
    /** 연락처 전화번호 */
    @Pattern(regexp = "^[0-9-]+$", message = "전화번호 형식이 올바르지 않습니다")
    @Schema(description = "연락처 전화번호", example = "02-1234-5678")
    private String contactPhone;
    
    /** 주소 */
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;
    
    /** 웹사이트 */
    @Pattern(regexp = "^https?://.*", message = "올바른 URL 형식이 아닙니다")
    @Schema(description = "웹사이트", example = "https://example.com")
    private String website;
    
    /** 최대 사용자 수 */
    @Min(value = 1, message = "최대 사용자 수는 1 이상이어야 합니다")
    @Schema(description = "최대 사용자 수", example = "100")
    private Integer maxUsers;
    
    /** 조직별 설정 (JSON) */
    @Schema(description = "조직별 설정 (JSON)", example = "{\"theme\": \"dark\"}")
    private String settings;
}
