package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.tenant.adapter.TenantIsolationAdapter;
import com.agenticcp.core.domain.tenant.adapter.TenantIsolationAdapterFactory;
import com.agenticcp.core.domain.tenant.adapter.dto.IsolationResult;
import com.agenticcp.core.domain.tenant.adapter.dto.IsolationStatus;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.domain.tenant.cloud.CloudProviderType;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.event.TenantIsolationAppliedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantIsolationService {

    private final TenantService tenantService;
    private final TenantIsolationAdapterFactory adapterFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 테넌트에 격리 정책을 적용합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param isolation 격리 정책 정보
     * @param cloudProviderType 클라우드 프로바이더 타입
     * @return 격리 정책 적용 결과
     * @throws BusinessException 테넌트를 찾을 수 없거나 격리 정책 적용에 실패한 경우
     */
    @Transactional
    public IsolationResult applyIsolationPolicy(String tenantKey, TenantIsolation isolation, 
                                               CloudProviderType cloudProviderType) {
        try {

            // Adapter 가져오기
            TenantIsolationAdapter adapter = adapterFactory.getAdapter(cloudProviderType);

            // Adapter 에게 위임
            IsolationResult result = adapter.applyIsolationPolicy(tenantKey, isolation);
            
            // 성공 시 이벤트 발행 (TenantIsolationEventListener가 비동기로 처리)
            if (result.success()) {
                eventPublisher.publishEvent(new TenantIsolationAppliedEvent(this, tenantKey, isolation));
                log.info("격리 정책이 성공적으로 적용되었습니다 - 테넌트: {}", tenantKey);
            } else {
                log.error("격리 정책 적용 실패 - 테넌트: {}, 사유: {}",
                         tenantKey, result.message());
                throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                    "격리 정책 적용에 실패했습니다: " + result.message());
            }
            
            return result;

        } catch (Exception e) {
            log.error("격리 정책 적용 중 오류 발생 - 테넌트: {}", tenantKey, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "격리 정책 적용 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 테넌트의 격리 정책을 제거합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param cloudProviderType 클라우드 프로바이더 타입
     * @throws BusinessException 격리 정책 제거에 실패한 경우
     */
    @Transactional
    public void removeIsolationPolicy(String tenantKey, CloudProviderType cloudProviderType) {
        try {
            TenantIsolationAdapter adapter = adapterFactory.getAdapter(cloudProviderType);

            adapter.removeIsolationPolicy(tenantKey);
            
            log.info("격리 정책이 성공적으로 제거되었습니다 - 테넌트: {}", tenantKey);

        } catch (Exception e) {
            log.error("격리 정책 제거 중 오류 발생 - 테넌트: {}", tenantKey, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "격리 정책 제거 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 테넌트의 격리 상태를 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param cloudProviderType 클라우드 프로바이더 타입
     * @return 격리 상태 정보
     * @throws BusinessException 격리 상태 조회에 실패한 경우
     */
    public IsolationStatus getIsolationStatus(String tenantKey, CloudProviderType cloudProviderType) {
        try {
            TenantIsolationAdapter adapter = adapterFactory.getAdapter(cloudProviderType);
            return adapter.getIsolationStatus(tenantKey);

        } catch (Exception e) {
            log.error("격리 상태 조회 중 오류 발생 - 테넌트: {}", tenantKey, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "격리 상태 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 격리 레벨을 지원하는 클라우드 프로바이더 목록을 조회합니다.
     * 
     * @param isolationLevel 격리 레벨
     * @return 지원하는 클라우드 프로바이더 목록
     * @throws BusinessException 지원 프로바이더 조회에 실패한 경우
     */
    public List<CloudProviderType> getSupportedProviders(TenantIsolation.IsolationLevel isolationLevel) {
        try {
            return adapterFactory.getAdaptersSupporting(isolationLevel).stream()
                    .map(adapter -> adapter.getSupportedCloudProvider())
                    .toList();
        } catch (Exception e) {
            log.error("지원하는 클라우드 프로바이더 조회 중 오류 발생 - 격리 레벨: {}", isolationLevel, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "지원하는 클라우드 프로바이더 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 테넌트의 격리 정책을 업데이트합니다.
     * 기존 정책을 제거하고 새로운 정책을 적용합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param newIsolation 새로운 격리 정책 정보
     * @param cloudProviderType 클라우드 프로바이더 타입
     * @return 격리 정책 적용 결과
     * @throws BusinessException 격리 정책 업데이트에 실패한 경우
     */
    @Transactional
    public IsolationResult updateIsolationPolicy(String tenantKey, TenantIsolation newIsolation, 
                                                CloudProviderType cloudProviderType) {
        try {
            log.info("격리 정책 업데이트 시작 - 테넌트: {}, 프로바이더: {}", tenantKey, cloudProviderType);
            
            // 1. 기존 격리 정책 제거
            try {
                removeIsolationPolicy(tenantKey, cloudProviderType);
            } catch (BusinessException e) {
                // 기존 정책이 없는 경우는 정상적인 상황일 수 있음
                log.warn("기존 격리 정책 제거 중 오류 발생 (무시됨) - 테넌트: {}, 오류: {}", 
                        tenantKey, e.getMessage());
            }
            
            // 2. 새로운 격리 정책 적용
            return applyIsolationPolicy(tenantKey, newIsolation, cloudProviderType);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("격리 정책 업데이트 중 오류 발생 - 테넌트: {}", tenantKey, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "격리 정책 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 테넌트의 격리 정책이 적용되어 있는지 확인합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param cloudProviderType 클라우드 프로바이더 타입
     * @return 격리 정책 적용 여부
     */
    public boolean isIsolationPolicyApplied(String tenantKey, CloudProviderType cloudProviderType) {
        try {
            IsolationStatus status = getIsolationStatus(tenantKey, cloudProviderType);
            return status.isActive();
        } catch (Exception e) {
            log.warn("격리 정책 적용 여부 확인 중 오류 발생 - 테넌트: {}, 오류: {}", 
                    tenantKey, e.getMessage());
            return false;
        }
    }
}
