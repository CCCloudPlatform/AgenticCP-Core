package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * RDBMS 생성 요청 DTO (CSP 중립적)
 * 
 * Controller에서 받는 요청 객체로, CSP 중립적인 필드만 포함합니다.
 * CSP 특화 설정은 providerSpecificConfig에 포함됩니다.
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
public class RdbmsCreateRequest {
    
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
     * 리전 (필수)
     * 예: us-east-1, ap-northeast-2
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;
    
    /**
     * 인스턴스 이름 (CSP 중립적)
     * - AWS: dbInstanceIdentifier
     * - Azure: serverName
     * - GCP: instanceId
     */
    @NotBlank(message = "인스턴스 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "인스턴스 이름은 1-100자 사이여야 합니다")
    private String instanceName;
    
    /**
     * 데이터베이스 엔진 (필수)
     * 예: mysql, postgresql, mariadb, oracle, sqlserver
     */
    @NotBlank(message = "데이터베이스 엔진은 필수입니다")
    private String engine;
    
    /**
     * 엔진 버전
     * CSP별 버전 형식이 다를 수 있음
     */
    private String engineVersion;
    
    /**
     * 인스턴스 크기 (CSP 중립적)
     * - AWS: db.t3.micro, db.t3.small 등
     * - Azure: GP_Gen5_2, BC_Gen5_2 등
     * - GCP: db-custom-2-7680 등
     */
    @NotBlank(message = "인스턴스 크기는 필수입니다")
    private String instanceSize;
    
    /**
     * 할당된 스토리지 크기 (GB)
     * 최소 20GB
     */
    @Min(value = 20, message = "스토리지 크기는 최소 20GB 이상이어야 합니다")
    private Integer allocatedStorage;
    
    /**
     * 관리자 사용자명 (필수)
     */
    @NotBlank(message = "관리자 사용자명은 필수입니다")
    private String masterUsername;
    
    /**
     * 관리자 패스워드 (필수)
     * 최소 8자 이상
     */
    @NotBlank(message = "관리자 패스워드는 필수입니다")
    @Size(min = 8, message = "패스워드는 최소 8자 이상이어야 합니다")
    private String masterPassword;
    
    /**
     * 초기 데이터베이스 이름
     */
    private String dbName;
    
    /**
     * 네트워크 보안 그룹 ID (CSP 중립적)
     * - AWS: securityGroupId
     * - Azure: firewallRule
     * - GCP: authorizedNetworks
     */
    private String networkSecurityId;
    
    /**
     * 서브넷 식별자 (선택적, 일부 CSP만 사용)
     */
    private String subnetId;
    
    /**
     * 데이터베이스 포트
     * 기본값: engine별로 다름 (MySQL: 3306, PostgreSQL: 5432 등)
     */
    private Integer port;
    
    /**
     * 가용 영역 (CSP 중립적)
     * - AWS: availabilityZone (예: us-east-1a)
     * - Azure: zone
     * - GCP: zone
     */
    private String zone;
    
    /**
     * 고가용성 설정
     * - AWS: multiAz
     * - Azure: highAvailability
     * - GCP: highAvailability
     */
    @Builder.Default
    private Boolean highAvailability = false;
    
    /**
     * 공개 접근 허용 여부
     */
    @Builder.Default
    private Boolean publiclyAccessible = false;
    
    /**
     * 태그
     */
    private Map<String, String> tags;
    
    /**
     * CSP별 특화 설정
     * 
     * AWS 예시:
     *   - subnetGroupName: "my-db-subnet-group"
     *   - parameterGroupName: "default.mysql8.0"
     * 
     * Azure 예시:
     *   - resourceGroupName: "my-resource-group"
     *   - sku: { "name": "GP_Gen5_2", "tier": "GeneralPurpose", "capacity": 2 }
     * 
     * GCP 예시:
     *   - databaseFlags: [{"name": "max_connections", "value": "100"}]
     *   - backupConfiguration: {"enabled": true, "startTime": "23:00"}
     */
    private Map<String, Object> providerSpecificConfig;
}
