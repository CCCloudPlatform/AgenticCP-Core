package com.agenticcp.core.domain.cloud.port.model.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * RDBMS 삭제 도메인 커맨드 (CSP 중립적)
 * 
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record RdbmsDeleteCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,
        Boolean skipSnapshot,           // 최종 스냅샷 건너뛰기 (AWS 특화, 다른 CSP는 providerSpecificConfig로)
        String snapshotName,            // 최종 스냅샷 이름 (선택적)
        Boolean deleteAutomatedBackups,  // 자동 백업 삭제 여부
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 삭제 옵션
        CloudSessionCredential session
) {
}
