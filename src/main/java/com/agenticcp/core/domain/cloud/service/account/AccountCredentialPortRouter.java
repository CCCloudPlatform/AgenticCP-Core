package com.agenticcp.core.domain.cloud.service.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudAccount;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.dto.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.dto.AccountValidationResponse;
import com.agenticcp.core.domain.cloud.dto.ConnectionTestResponse;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountSyncPort;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountValidationPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AccountCredentialPortRouter
 *
 * AccountCredentialManagementPort, AccountSyncPort, AccountValidationPort 구현체를
 * 프로바이더 타입에 따라 라우팅합니다.
 */
@Slf4j
@Component
@Primary
public class AccountCredentialPortRouter implements
        AccountCredentialManagementPort,
        AccountSyncPort,
        AccountValidationPort {

    private final CloudAccountRepository cloudAccountRepository;
    private final Map<ProviderType, AccountCredentialManagementPort> credentialPorts;
    private final Map<ProviderType, AccountSyncPort> syncPorts;
    private final Map<ProviderType, AccountValidationPort> validationPorts;

    public AccountCredentialPortRouter(
            CloudAccountRepository cloudAccountRepository,
            List<AccountCredentialManagementPort> credentialPorts,
            List<AccountSyncPort> syncPorts,
            List<AccountValidationPort> validationPorts
    ) {
        this.cloudAccountRepository = cloudAccountRepository;
        this.credentialPorts = toProviderMap(credentialPorts, "AccountCredentialManagementPort");
        this.syncPorts = toProviderMap(syncPorts, "AccountSyncPort");
        this.validationPorts = toProviderMap(validationPorts, "AccountValidationPort");

        log.info("[AccountCredentialPortRouter] 초기화 - credentialPorts={}, syncPorts={}, validationPorts={}",
                this.credentialPorts.keySet(), this.syncPorts.keySet(), this.validationPorts.keySet());
    }

    // AccountCredentialManagementPort 구현
    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        return credential(providerType).resolveCredentials(tenantKey, providerType, accountScope);
    }

    @Override
    public String storeCredentials(String tenantKey, ProviderType providerType, String accountScope, Map<String, String> credentials) {
        return credential(providerType).storeCredentials(tenantKey, providerType, accountScope, credentials);
    }

    @Override
    public void deleteCredentials(ProviderType providerType, String credentialKey) {
        credential(providerType).deleteCredentials(providerType, credentialKey);
    }

    @Override
    public CloudSessionCredential getSession(String tenantKey, String accountScope, ProviderType providerType) {
        return credential(providerType).getSession(tenantKey, accountScope, providerType);
    }

    // AccountSyncPort 구현
    @Override
    public CloudAccount syncAccountInfo(Long accountId) {
        ProviderType providerType = getProviderType(accountId);
        AccountSyncPort port = getPort(syncPorts, providerType, "AccountSyncPort");
        return port.syncAccountInfo(accountId);
    }

    // AccountValidationPort 구현
    @Override
    public AccountValidationResponse validateAccount(AccountValidationRequest request) {
        ProviderType providerType = request.getProviderType();
        AccountValidationPort port = getPort(validationPorts, providerType, "AccountValidationPort");
        return port.validateAccount(request);
    }

    @Override
    public ConnectionTestResponse testConnection(Long accountId, Map<String, String> credentials) {
        ProviderType providerType = getProviderType(accountId);
        AccountValidationPort port = getPort(validationPorts, providerType, "AccountValidationPort");
        return port.testConnection(accountId, credentials);
    }

    public AccountCredentialManagementPort credential(ProviderType providerType) {
        return getPort(credentialPorts, providerType, "AccountCredentialManagementPort");
    }

    public AccountSyncPort sync(ProviderType providerType) {
        return getPort(syncPorts, providerType, "AccountSyncPort");
    }

    public AccountValidationPort validation(ProviderType providerType) {
        return getPort(validationPorts, providerType, "AccountValidationPort");
    }

    private <T> Map<ProviderType, T> toProviderMap(List<T> ports, String portTypeName) {
        return ports.stream()
                .filter(port -> port instanceof ProviderScoped)
                .collect(Collectors.toMap(
                        port -> ((ProviderScoped) port).getProviderType(),
                        port -> port,
                        (existing, replacement) -> {
                            log.warn("[AccountCredentialPortRouter] 중복된 {} 발견: provider={}, existing={}, replacement={}",
                                    portTypeName,
                                    ((ProviderScoped) existing).getProviderType(),
                                    existing.getClass().getSimpleName(),
                                    replacement.getClass().getSimpleName());
                            return existing;
                        }
                ));
    }

    private <T> T getPort(Map<ProviderType, T> ports, ProviderType providerType, String portTypeName) {
        T adapter = ports.get(providerType);
        if (adapter == null) {
            throw new IllegalArgumentException(
                    String.format("%s 를 지원하지 않는 프로바이더입니다: %s", portTypeName, providerType));
        }
        return adapter;
    }

    private ProviderType getProviderType(Long accountId) {
        return cloudAccountRepository.findById(accountId)
                .map(account -> account.getProvider().getProviderType())
                .orElseThrow(() -> new BusinessException(
                        CloudErrorCode.ACCOUNT_NOT_FOUND,
                        "계정을 찾을 수 없습니다: " + accountId
                ));
    }
}
