package com.agenticcp.core.domain.tenant.adapter;

import com.agenticcp.core.domain.tenant.controller.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * TenantIsolationAdapter 인스턴스를 생성하는 Factory
 * Adapter 패턴의 Factory 역할
 */
@Component
public class TenantIsolationAdapterFactory {
    
    private final Map<CloudProviderType, TenantIsolationAdapter> adapters;
    
    public TenantIsolationAdapterFactory(List<TenantIsolationAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        adapter -> CloudProviderType.valueOf(adapter.getSupportedCloudProvider()),
                        Function.identity()
                ));
    }
    
    /**
     * 클라우드 프로바이더 타입에 해당하는 Adapter를 반환합니다.
     * 
     * @param providerType 클라우드 프로바이더 타입
     * @return 해당하는 Adapter 인스턴스
     * @throws IllegalArgumentException 지원하지 않는 프로바이더 타입인 경우
     */
    public TenantIsolationAdapter getAdapter(CloudProviderType providerType) {
        TenantIsolationAdapter adapter = adapters.get(providerType);
        if (adapter == null) {
            throw new IllegalArgumentException("Unsupported cloud provider type: " + providerType);
        }
        return adapter;
    }
    
    /**
     * 특정 격리 레벨을 지원하는 Adapter 목록을 반환합니다.
     * 
     * @param isolationLevel 격리 레벨
     * @return 해당 격리 레벨을 지원하는 Adapter 목록
     */
    public List<TenantIsolationAdapter> getAdaptersSupporting(TenantIsolation.IsolationLevel isolationLevel) {
        return adapters.values().stream()
                .filter(adapter -> adapter.supportsIsolationLevel(isolationLevel))
                .toList();
    }
    
    /**
     * 사용 가능한 모든 Adapter 목록을 반환합니다.
     * 
     * @return 모든 Adapter 목록
     */
    public List<TenantIsolationAdapter> getAllAdapters() {
        return List.copyOf(adapters.values());
    }
}