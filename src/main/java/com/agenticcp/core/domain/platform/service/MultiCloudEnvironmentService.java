package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.service.CloudResourceService;
import com.agenticcp.core.domain.platform.enums.MultiCloudEnvironment;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀티클라우드 환경 감지 서비스
 * 
 * 테넌트가 사용 중인 클라우드 리소스를 분석하여
 * 멀티클라우드 환경 타입을 자동으로 감지합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiCloudEnvironmentService {
    
    private final CloudResourceService cloudResourceService;
    
    /**
     * 테넌트의 멀티클라우드 환경 감지
     * 
     * @param tenantId 테넌트 ID
     * @return 감지된 환경 타입
     */
    public MultiCloudEnvironment detectEnvironment(String tenantId) {
        log.info("[MultiCloudEnvironmentService] detectEnvironment - tenantId={}", 
                LogMaskingUtils.mask(tenantId, 2, 2));
        
        // 테넌트의 클라우드 리소스 조회
        List<CloudResource> resources = cloudResourceService.getResourcesByTenant(tenantId);
        
        if (resources == null || resources.isEmpty()) {
            log.warn("[MultiCloudEnvironmentService] No cloud resources found for tenant: {}", 
                    LogMaskingUtils.mask(tenantId, 2, 2));
            return MultiCloudEnvironment.ON_PREMISE; // 리소스 없으면 온프레미스로 간주
        }
        
        // 프로바이더 타입 추출
        Set<String> providers = resources.stream()
                .map(r -> r.getProvider().getProviderType().name())
                .collect(Collectors.toSet());
        
        log.debug("[MultiCloudEnvironmentService] Detected providers: {}", providers);
        
        // 환경 타입 결정
        MultiCloudEnvironment environment = determineEnvironment(providers);
        
        log.info("[MultiCloudEnvironmentService] detectEnvironment - result={} tenantId={}", 
                environment, LogMaskingUtils.mask(tenantId, 2, 2));
        
        return environment;
    }
    
    /**
     * 프로바이더 집합으로부터 환경 타입 결정
     * 
     * @param providers 프로바이더 타입 집합
     * @return 환경 타입
     */
    private MultiCloudEnvironment determineEnvironment(Set<String> providers) {
        boolean hasOnPremise = providers.contains("ON_PREMISE");
        boolean hasCloud = providers.stream()
                .anyMatch(p -> !p.equals("ON_PREMISE"));
        
        if (hasOnPremise && hasCloud) {
            // 온프레미스 + 클라우드 = 하이브리드
            return MultiCloudEnvironment.HYBRID;
        } else if (hasOnPremise) {
            // 온프레미스만 = 온프레미스
            return MultiCloudEnvironment.ON_PREMISE;
        } else if (providers.size() > 1) {
            // 2개 이상의 클라우드 = 멀티클라우드
            return MultiCloudEnvironment.MULTI_CLOUD;
        } else {
            // 1개의 클라우드 = 싱글클라우드
            return MultiCloudEnvironment.SINGLE_CLOUD;
        }
    }
}

