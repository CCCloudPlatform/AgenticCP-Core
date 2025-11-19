package com.agenticcp.core.domain.cloud.mapper;

import com.agenticcp.core.domain.cloud.port.model.account.*;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 자격증명 관련 DTO → Command 변환 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class CredentialCommandMapper {
    
    /**
     * RegisterCloudAccountRequest에서 StoreCredentialCommand로 변환
     * 
     * @param tenantKey 테넌트 키
     * @param request 계정 등록 요청
     * @return StoreCredentialCommand
     */
    public StoreCredentialCommand toStoreCommand(String tenantKey, RegisterCloudAccountRequest request) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("accessKeyId", request.getAccessKey());
        credentials.put("secretAccessKey", request.getSecretKey());
        if (request.getRegion() != null) {
            credentials.put("region", request.getRegion());
        }
        
        return StoreCredentialCommand.builder()
                .tenantKey(tenantKey)
                .providerType(request.getProviderType())
                .accountScope(request.getAccountId())
                .credentials(credentials)
                .build();
    }
    
    /**
     * 자격증명 저장 커맨드 생성
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위
     * @param accessKey Access Key
     * @param secretKey Secret Key
     * @param region 리전
     * @return StoreCredentialCommand
     */
    public StoreCredentialCommand toStoreCommand(String tenantKey, ProviderType providerType, 
                                                  String accountScope, String accessKey, 
                                                  String secretKey, String region) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("accessKeyId", accessKey);
        credentials.put("secretAccessKey", secretKey);
        if (region != null) {
            credentials.put("region", region);
        }
        
        return StoreCredentialCommand.builder()
                .tenantKey(tenantKey)
                .providerType(providerType)
                .accountScope(accountScope)
                .credentials(credentials)
                .build();
    }
    
    /**
     * 자격증명 조회 커맨드 생성
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위
     * @return ResolveCredentialCommand
     */
    public ResolveCredentialCommand toResolveCommand(String tenantKey, ProviderType providerType, 
                                                      String accountScope) {
        return ResolveCredentialCommand.builder()
                .tenantKey(tenantKey)
                .providerType(providerType)
                .accountScope(accountScope)
                .build();
    }
    
    /**
     * 자격증명 삭제 커맨드 생성
     * 
     * @param providerType 프로바이더 타입
     * @param credentialKey 자격증명 키
     * @return DeleteCredentialCommand
     */
    public DeleteCredentialCommand toDeleteCommand(ProviderType providerType, String credentialKey) {
        return DeleteCredentialCommand.builder()
                .providerType(providerType)
                .credentialKey(credentialKey)
                .build();
    }
}

