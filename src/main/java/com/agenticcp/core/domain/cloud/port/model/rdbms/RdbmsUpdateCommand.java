package com.agenticcp.core.domain.cloud.port.model.rdbms;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.Map;

/**
 * RDBMS 수정 도메인 커맨드 (CSP 중립적)
 * 
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record RdbmsUpdateCommand(
        CloudProvider.ProviderType providerType,
        String accountScope,
        String region,
        String providerResourceId,   // RDBMS 인스턴스 ID (CSP별 형식 다를 수 있음)
        String instanceSize,         // CSP 중립적: 인스턴스 크기 변경
        Integer allocatedStorage,    // 스토리지 크기 변경 (GB)
        String adminPassword,         // 선택적: 관리자 패스워드 변경
        Boolean applyImmediately,    // 즉시 적용 여부 (일부 CSP만 지원)
        Map<String, String> tags,
        String tenantKey,
        Map<String, Object> providerSpecificConfig,  // CSP별 특화 설정
        CloudSessionCredential session
) {
}
