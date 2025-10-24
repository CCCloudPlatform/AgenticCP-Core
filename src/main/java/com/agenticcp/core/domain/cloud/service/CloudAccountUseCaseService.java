package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.domain.cloud.dto.*;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.mapper.CloudAccountMapper;
import com.agenticcp.core.domain.cloud.port.model.AccountMetadata;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.AccountValidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountCredentialPort;
import com.agenticcp.core.domain.cloud.port.outbound.CloudAccountValidationPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.common.util.LogMaskingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 클라우드 계정 관리 유즈케이스 서비스
 * 
 * 비즈니스 유즈케이스를 조율하고 Port 인터페이스를 통해 외부 시스템과 연동합니다.
 * 멀티 클라우드 환경에서 동일한 비즈니스 로직을 제공합니다.
 * 
 * 주요 기능:
 * - 계정 등록 플로우 (검증 → 저장 → 감사로그)
 * - 계정 연결 테스트
 * - 멀티 클라우드 통합 조회
 * - 계정 동기화
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CloudAccountUseCaseService {
    
    private final CloudAccountService cloudAccountService;
    private final CloudProviderService cloudProviderService;
    private final CloudAccountMapper cloudAccountMapper;
    
    // Port 인터페이스들
    private final CloudAccountValidationPort validationPort;
    @SuppressWarnings("unused") // Phase 7에서 사용 예정
    private final CloudAccountCredentialPort credentialPort;
    private final AuditEventPort auditEventPort;
    private final TracingPort tracingPort;
    
    // ==================== 계정 등록 유즈케이스 ====================
    
    /**
     * AWS 클라우드 계정을 등록합니다.
     * 
     * 플로우:
     * 1. 입력 데이터 검증 (DTO 레벨에서 이미 완료)
     * 2. CloudAccountValidationPort로 실제 계정 검증
     * 3. 검증 성공 시 CloudAccountService로 저장
     * 4. CredentialProviderPort로 인증 정보 안전하게 저장
     * 5. AuditEventPort로 감사 로그 기록
     * 
     * @param request AWS 계정 등록 요청
     * @return 등록된 계정 정보
     */
    @Transactional
    public CloudAccountDto registerAwsAccount(RegisterAwsAccountRequest request) {
        log.info("[CloudAccountUseCaseService] registerAwsAccount - tenantId={}, accountId={}", 
                request.getTenantId(), LogMaskingUtils.mask(request.getAccountId(), 2, 2));
        
        try (AutoCloseable span = tracingPort.startSpan("registerAwsAccount", Map.of("tenantId", request.getTenantId().toString()))) {
            // 1. Provider 정보 조회
            CloudProvider provider = cloudProviderService.getProviderByKeyOrThrow("aws");
            
            // 2. 계정 검증 요청 생성
            AccountValidationRequest validationRequest = createAwsValidationRequest(request, provider);
            
            // 3. 실제 계정 검증
            AccountValidationResult validationResult = validationPort.validateAccount(validationRequest);
            
            if (!validationResult.isValid()) {
                log.warn("[CloudAccountUseCaseService] registerAwsAccount - validation failed: {}", 
                        validationResult.getMessage());
                throw new BusinessException(CloudErrorCode.ACCOUNT_VALIDATION_FAILED);
            }
            
            // 4. CloudAccount 엔티티 생성
            CloudAccount cloudAccount = createAwsCloudAccount(request, provider, validationResult.getAccountMetadata());
            
            // 5. 계정 저장
            CloudAccount savedAccount = cloudAccountService.createAccount(cloudAccount);
            
            // 6. 인증 정보 저장 (IAM Role ARN 등)
            storeAwsCredentials(request, savedAccount);
            
            // 7. 감사 로그 기록
            auditEventPort.record("CLOUD_ACCOUNT_REGISTERED", 
                    "CloudAccount", "SUCCESS",
                    Map.of("accountId", savedAccount.getId() != null ? savedAccount.getId() : 0L, 
                           "providerType", "AWS",
                           "tenantId", request.getTenantId() != null ? request.getTenantId() : 0L));
            
            log.info("[CloudAccountUseCaseService] registerAwsAccount - success id={}", savedAccount.getId());
            return cloudAccountMapper.toDto(savedAccount);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] registerAwsAccount - error", e);
            throw new RuntimeException("AWS 계정 등록 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * GCP 클라우드 계정을 등록합니다.
     * 
     * @param request GCP 계정 등록 요청
     * @return 등록된 계정 정보
     */
    @Transactional
    public CloudAccountDto registerGcpAccount(RegisterGcpAccountRequest request) {
        log.info("[CloudAccountUseCaseService] registerGcpAccount - tenantId={}, accountId={}", 
                request.getTenantId(), LogMaskingUtils.mask(request.getAccountId(), 2, 2));
        
        try (AutoCloseable span = tracingPort.startSpan("registerGcpAccount", Map.of("tenantId", request.getTenantId().toString()))) {
            // TODO: Phase 7에서 GCP 어댑터 구현 예정
            throw new UnsupportedOperationException("GCP 계정 등록은 Phase 7에서 구현 예정입니다.");
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] registerGcpAccount - error", e);
            throw new RuntimeException("GCP 계정 등록 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * Azure 클라우드 계정을 등록합니다.
     * 
     * @param request Azure 계정 등록 요청
     * @return 등록된 계정 정보
     */
    @Transactional
    public CloudAccountDto registerAzureAccount(RegisterAzureAccountRequest request) {
        log.info("[CloudAccountUseCaseService] registerAzureAccount - tenantId={}, accountId={}", 
                request.getTenantId(), LogMaskingUtils.mask(request.getAccountId(), 2, 2));
        
        try (AutoCloseable span = tracingPort.startSpan("registerAzureAccount", Map.of("tenantId", request.getTenantId().toString()))) {
            // TODO: Phase 7에서 Azure 어댑터 구현 예정
            throw new UnsupportedOperationException("Azure 계정 등록은 Phase 7에서 구현 예정입니다.");
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] registerAzureAccount - error", e);
            throw new RuntimeException("Azure 계정 등록 중 오류가 발생했습니다.", e);
        }
    }
    
    // ==================== 계정 연결 테스트 유즈케이스 ====================
    
    /**
     * 클라우드 계정의 연결 상태를 테스트합니다.
     * 
     * @param accountId 계정 ID
     * @return 연결 테스트 결과
     */
    public ConnectionTestResult testConnection(Long accountId) {
        log.info("[CloudAccountUseCaseService] testConnection - accountId={}", accountId);
        
        try (AutoCloseable span = tracingPort.startSpan("testConnection", Map.of("accountId", accountId.toString()))) {
            // 1. 계정 정보 조회
            CloudAccount account = cloudAccountService.getAccountByIdOrThrow(accountId);
            
            // 2. 검증 요청 생성
            AccountValidationRequest validationRequest = createConnectionTestRequest(account);
            
            // 3. 연결 테스트 실행 (실제 AWS API 호출)
            AccountValidationResult result = validationPort.testConnection(validationRequest);
            
            // 4. 결과에 따라 계정 상태 업데이트
            if (result.isValid()) {
                cloudAccountService.updateLastVerified(accountId);
                log.info("[CloudAccountUseCaseService] testConnection - success accountId={}", accountId);
            } else {
                log.warn("[CloudAccountUseCaseService] testConnection - failed accountId={}, message={}", 
                        accountId, result.getMessage());
            }
            
            // 5. 감사 로그 기록
            auditEventPort.record("CLOUD_ACCOUNT_CONNECTION_TESTED", 
                    "CloudAccount", result.isValid() ? "SUCCESS" : "FAILURE",
                    Map.of("accountId", accountId,
                           "success", result.isValid(),
                           "message", result.getMessage() != null ? result.getMessage() : ""));
            
            return ConnectionTestResult.builder()
                    .connected(result.isValid())
                    .accountId(account.getId())
                    .cloudAccountId(account.getAccountId())
                    .providerType(account.getProvider().getProviderType().toString())
                    .message(result.getMessage())
                    .testedAt(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] testConnection - error accountId={}", accountId, e);
            throw new RuntimeException("연결 테스트 중 오류가 발생했습니다.", e);
        }
    }
    
    // ==================== 멀티 클라우드 통합 조회 유즈케이스 ====================
    
    /**
     * 테넌트의 모든 클라우드 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @return 클라우드 계정 목록
     */
    public List<CloudAccountDto> getMultiCloudAccounts(Long tenantId) {
        log.info("[CloudAccountUseCaseService] getMultiCloudAccounts - tenantId={}", tenantId);
        
        try (AutoCloseable span = tracingPort.startSpan("getMultiCloudAccounts", Map.of("tenantId", tenantId.toString()))) {
            List<CloudAccount> accounts = cloudAccountService.getAccountsByTenantId(tenantId);
            
            List<CloudAccountDto> accountDtos = accounts.stream()
                    .map(cloudAccountMapper::toDto)
                    .collect(Collectors.toList());
            
            log.info("[CloudAccountUseCaseService] getMultiCloudAccounts - success count={} tenantId={}", 
                    accountDtos.size(), tenantId);
            
            return accountDtos;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] getMultiCloudAccounts - error tenantId={}", tenantId, e);
            throw new RuntimeException("멀티 클라우드 계정 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 특정 프로바이더의 클라우드 계정을 조회합니다.
     * 
     * @param tenantId 테넌트 ID
     * @param providerType 프로바이더 타입
     * @return 클라우드 계정 목록
     */
    public List<CloudAccountDto> getCloudAccountsByProvider(Long tenantId, CloudProvider.ProviderType providerType) {
        log.info("[CloudAccountUseCaseService] getCloudAccountsByProvider - tenantId={}, providerType={}", 
                tenantId, providerType);
        
        try (AutoCloseable span = tracingPort.startSpan("getCloudAccountsByProvider", 
                Map.of("tenantId", tenantId.toString(), "providerType", providerType.toString()))) {
            // Provider 정보 조회
            CloudProvider provider = cloudProviderService.getProvidersByType(providerType).stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("CloudProvider", "providerType", providerType.toString()));
            
            List<CloudAccount> accounts = cloudAccountService.getAccountsByTenantIdAndProviderId(tenantId, provider.getId());
            
            List<CloudAccountDto> accountDtos = accounts.stream()
                    .map(cloudAccountMapper::toDto)
                    .collect(Collectors.toList());
            
            log.info("[CloudAccountUseCaseService] getCloudAccountsByProvider - success count={} tenantId={} providerType={}", 
                    accountDtos.size(), tenantId, providerType);
            
            return accountDtos;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] getCloudAccountsByProvider - error tenantId={} providerType={}", 
                    tenantId, providerType, e);
            throw new RuntimeException("프로바이더별 계정 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    // ==================== 계정 동기화 유즈케이스 ====================
    
    /**
     * 모든 활성 계정의 연결 상태를 동기화합니다.
     * 
     * @param tenantId 테넌트 ID
     * @return 동기화 결과 요약
     */
    @Transactional
    public Map<String, Object> syncAllAccounts(Long tenantId) {
        log.info("[CloudAccountUseCaseService] syncAllAccounts - tenantId={}", tenantId);
        
        try (AutoCloseable span = tracingPort.startSpan("syncAllAccounts", Map.of("tenantId", tenantId.toString()))) {
            List<CloudAccount> accounts = cloudAccountService.getAccountsByTenantId(tenantId);
            
            int successCount = 0;
            int failureCount = 0;
            
            for (CloudAccount account : accounts) {
                try {
                    ConnectionTestResult result = testConnection(account.getId());
                    if (result.isConnected()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.warn("[CloudAccountUseCaseService] syncAllAccounts - failed accountId={}", account.getId(), e);
                    failureCount++;
                }
            }
            
            Map<String, Object> syncResult = Map.of(
                    "totalAccounts", accounts.size(),
                    "successCount", successCount,
                    "failureCount", failureCount,
                    "syncedAt", LocalDateTime.now()
            );
            
            // 감사 로그 기록
            auditEventPort.record("CLOUD_ACCOUNTS_SYNCED", 
                    "CloudAccount", "SUCCESS",
                    Map.of("tenantId", tenantId,
                           "totalAccounts", accounts.size(),
                           "successCount", successCount,
                           "failureCount", failureCount));
            
            log.info("[CloudAccountUseCaseService] syncAllAccounts - success tenantId={} total={} success={} failure={}", 
                    tenantId, accounts.size(), successCount, failureCount);
            
            return syncResult;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] syncAllAccounts - error tenantId={}", tenantId, e);
            throw new RuntimeException("계정 동기화 중 오류가 발생했습니다.", e);
        }
    }
    
    // ==================== Private Helper Methods ====================
    
    private AccountValidationRequest createAwsValidationRequest(RegisterAwsAccountRequest request, CloudProvider provider) {
        return AccountValidationRequest.builder()
                .providerType(provider.getProviderType())
                .accountId(request.getAccountId())
                .region(request.getRegion())
                .authMethod(request.getAuthMethod())
                .roleArn(request.getRoleArn())
                .externalId(request.getExternalId())
                .additionalMetadata(Map.of("roleArn", request.getRoleArn() != null ? request.getRoleArn() : "", 
                                          "externalId", request.getExternalId() != null ? request.getExternalId() : ""))
                .build();
    }
    
    private CloudAccount createAwsCloudAccount(RegisterAwsAccountRequest request, CloudProvider provider, AccountMetadata metadata) {
        // TODO: CloudAccount 엔티티 생성 로직 구현
        // 현재는 기본 구조만 제공
        return CloudAccount.builder()
                .accountId(request.getAccountId())
                .accountName(request.getAccountName())
                .description(request.getDescription())
                .authMethod(request.getAuthMethod())
                .defaultRegion(request.getRegion())
                .isDefault(request.getIsDefault())
                .build();
    }
    
    private void storeAwsCredentials(RegisterAwsAccountRequest request, CloudAccount account) {
        // TODO: AWS 인증 정보 저장 로직 구현
        log.info("[CloudAccountUseCaseService] storeAwsCredentials - accountId={}", account.getId());
    }
    
    private AccountValidationRequest createConnectionTestRequest(CloudAccount account) {
        return AccountValidationRequest.builder()
                .providerType(account.getProvider().getProviderType())
                .accountId(account.getAccountId())
                .region(account.getDefaultRegion())
                .authMethod(account.getAuthMethod())
                .roleArn(account.getRoleArn())
                .externalId(account.getExternalId())
                .additionalMetadata(Map.of())
                .build();
    }
    
    /**
     * 클라우드 계정 정보를 수정합니다.
     * 
     * @param accountId 계정 ID
     * @param request 수정 요청 정보
     * @return 수정된 계정 정보
     */
    @Transactional
    public CloudAccountDto updateAccount(Long accountId, UpdateCloudAccountRequest request) {
        log.info("[CloudAccountUseCaseService] updateAccount - accountId={}", accountId);
        
        try (AutoCloseable span = tracingPort.startSpan("CloudAccountUseCaseService.updateAccount", Map.of())) {
            CloudAccount account = cloudAccountService.getAccountByIdOrThrow(accountId);
            
            // 계정 정보 수정
            if (request.getAccountName() != null) {
                account.setAccountName(request.getAccountName());
            }
            if (request.getDescription() != null) {
                account.setDescription(request.getDescription());
            }
            if (request.getDefaultRegion() != null) {
                account.setDefaultRegion(request.getDefaultRegion());
            }
            if (request.getIsDefault() != null) {
                account.setIsDefault(request.getIsDefault());
            }
            
            CloudAccount saved = cloudAccountService.updateAccount(accountId, account);
            
            // 감사 로그 기록
            auditEventPort.record("CLOUD_ACCOUNT_UPDATED", 
                    "CloudAccount", "SUCCESS",
                    Map.of("accountId", accountId,
                           "providerType", account.getProvider().getProviderType().toString(),
                           "tenantId", account.getTenant() != null ? account.getTenant().getId() : 0L));
            
            log.info("[CloudAccountUseCaseService] updateAccount - success accountId={}", accountId);
            return cloudAccountMapper.toDto(saved);
            
        } catch (ResourceNotFoundException e) {
            log.error("[CloudAccountUseCaseService] updateAccount - resource not found accountId={}: {}", accountId, e.getMessage());
            throw e;
        } catch (BusinessException e) {
            log.error("[CloudAccountUseCaseService] updateAccount - business error accountId={}: {}", accountId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] updateAccount - unexpected error accountId={}: {}", accountId, e.getMessage());
            throw new RuntimeException("Failed to update cloud account", e);
        }
    }
    
    /**
     * 클라우드 계정을 삭제합니다.
     * 
     * @param accountId 계정 ID
     * @return 삭제 성공 여부
     */
    @Transactional
    public boolean deleteAccount(Long accountId) {
        log.info("[CloudAccountUseCaseService] deleteAccount - accountId={}", accountId);
        
        try (AutoCloseable span = tracingPort.startSpan("CloudAccountUseCaseService.deleteAccount", Map.of())) {
            CloudAccount account = cloudAccountService.getAccountByIdOrThrow(accountId);
            
            // 계정 삭제
            cloudAccountService.deleteAccount(accountId);
            
            // 감사 로그 기록
            auditEventPort.record("CLOUD_ACCOUNT_DELETED", 
                    "CloudAccount", "SUCCESS",
                    Map.of("accountId", accountId,
                           "providerType", account.getProvider().getProviderType().toString(),
                           "tenantId", account.getTenant() != null ? account.getTenant().getId() : 0L));
            
            log.info("[CloudAccountUseCaseService] deleteAccount - success accountId={}", accountId);
            return true;
            
        } catch (ResourceNotFoundException e) {
            log.error("[CloudAccountUseCaseService] deleteAccount - resource not found accountId={}: {}", accountId, e.getMessage());
            throw e;
        } catch (BusinessException e) {
            log.error("[CloudAccountUseCaseService] deleteAccount - business error accountId={}: {}", accountId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[CloudAccountUseCaseService] deleteAccount - unexpected error accountId={}: {}", accountId, e.getMessage());
            throw new RuntimeException("Failed to delete cloud account", e);
        }
    }
}

