package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.enums.AccountStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 클라우드 계정 수정 요청 DTO
 * 등록된 클라우드 계정 정보를 수정할 때 사용됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCloudAccountRequest {
    
    /**
     * 계정 이름 (사용자 지정)
     */
    @Size(min = 2, max = 100, message = "계정 이름은 2-100자 사이여야 합니다")
    private String accountName;
    
    /**
     * 계정 상태
     */
    private AccountStatus accountStatus;
    
    /**
     * 기본 계정 설정 여부
     */
    private Boolean isDefault;
    
    /**
     * 기본 리전
     */
    private String region;
    
    /**
     * 추가 메타데이터
     */
    private Map<String, String> metadata;
}

