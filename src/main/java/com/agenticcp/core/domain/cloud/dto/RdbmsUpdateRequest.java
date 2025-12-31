package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * RDBMS 수정 요청 DTO (CSP 중립적)
 * 
 * RDBMS 인스턴스의 수정을 위한 요청 객체입니다.
 * CSP 중립적인 필드를 사용하며, CSP 특화 설정은 providerSpecificConfig에 포함됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RdbmsUpdateRequest {
    
    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;
    
    /**
     * 수정할 인스턴스 ID
     */
    private String instanceId;
    
    /**
     * 인스턴스 크기 변경 (CSP 중립적)
     * - AWS: db.t3.micro → db.t3.small
     * - Azure: GP_Gen5_2 → GP_Gen5_4
     * - GCP: db-custom-2-7680 → db-custom-4-15360
     */
    private String instanceSize;
    
    /**
     * 스토리지 크기 변경 (GB)
     * 최소 20GB, 증가만 가능 (일부 CSP는 감소 불가)
     */
    @Min(value = 20, message = "스토리지 크기는 최소 20GB 이상이어야 합니다")
    private Integer allocatedStorage;
    
    /**
     * 관리자 패스워드 변경 (선택적)
     * 최소 8자 이상
     */
    @Size(min = 8, message = "패스워드는 최소 8자 이상이어야 합니다")
    private String masterPassword;
    
    /**
     * 즉시 적용 여부
     * 일부 CSP만 지원 (AWS는 지원, 일부 CSP는 다음 유지보수 창에 적용)
     */
    private Boolean applyImmediately;
    
    /**
     * 추가할 태그
     */
    private Map<String, String> tagsToAdd;
    
    /**
     * 제거할 태그
     */
    private Map<String, String> tagsToRemove;
    
    /**
     * CSP별 특화 설정
     */
    private Map<String, Object> providerSpecificConfig;
}
