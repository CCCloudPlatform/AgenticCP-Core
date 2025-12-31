package com.agenticcp.core.domain.cloud.port.model.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * RDBMS 생성 도메인 커맨드 (CSP 중립적)
 * 
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 * 
 * 필드 매핑 예시:
 * - instanceName: AWS(dbInstanceIdentifier), Azure(serverName), GCP(instanceId)
 * - instanceSize: AWS(db.t3.micro), Azure(GP_Gen5_2), GCP(db-custom-2-7680)
 * - networkSecurityId: AWS(securityGroupId), Azure(firewallRule), GCP(authorizedNetworks)
 * - zone: AWS(availabilityZone), Azure(zone), GCP(zone)
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record RdbmsCreateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String serviceKey,           // "RDS", "AZURE_DATABASE", "CLOUD_SQL"
        String resourceType,         // "DATABASE"
        String instanceName,         // CSP 중립적: AWS(dbInstanceIdentifier), Azure(serverName), GCP(instanceId)
        String engine,               // "mysql", "postgresql", "mariadb", "oracle", "sqlserver"
        String engineVersion,       // CSP별 버전 형식 다를 수 있음
        String instanceSize,        // CSP 중립적: AWS(db.t3.micro), Azure(GP_Gen5_2), GCP(db-custom-2-7680)
        Integer allocatedStorage,    // GB (모든 CSP 공통)
        String adminUsername,         // 관리자 사용자명
        String adminPassword,         // 암호화 필요
        String dbName,               // 초기 데이터베이스 이름
        String networkSecurityId,   // CSP 중립적: AWS(securityGroupId), Azure(firewallRule), GCP(authorizedNetworks)
        String subnetId,            // 서브넷 식별자 (선택적, 일부 CSP만 사용)
        Integer port,               // 데이터베이스 포트 (기본값: engine별로 다름)
        String zone,                // CSP 중립적: AWS(availabilityZone), Azure(zone), GCP(zone)
        Boolean highAvailability,   // 고가용성 설정: AWS(multiAz), Azure(highAvailability), GCP(highAvailability)
        Boolean publiclyAccessible, // 공개 접근 허용 여부
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 설정
        CloudSessionCredential session
) {
}
