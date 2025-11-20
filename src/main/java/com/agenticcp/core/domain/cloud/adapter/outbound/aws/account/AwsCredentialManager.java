package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.masking.Masked;
import com.agenticcp.core.common.logging.masking.MaskingType;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AWS 자격증명 관리 서비스
 * AWS 자격증명을 암호화하여 저장하고 조회하는 기능을 제공합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsCredentialManager {

    private final EncryptionService encryptionService;
    private final CloudAccountCredentialRepository credentialRepository;
    private final MaskingService maskingService;

    /**
     * AWS 자격증명을 암호화하여 저장합니다.
     *
     * @param tenantKey       테넌트 키
     * @param accessKeyId     AWS Access Key ID
     * @param secretAccessKey AWS Secret Access Key
     * @param region          기본 리전
     * @return 저장된 CloudAccountCredential
     * @throws BusinessException 암호화 또는 저장 실패 시
     */
    @Transactional
    public CloudAccountCredential storeCredentials(String tenantKey, String accessKeyId,
                                                   String secretAccessKey, String region) {
        String maskedTenantKey = maskingService.maskTenantKey(tenantKey);
        log.info("[AwsCredentialManager] storeCredentials - tenantKey={}, region={}", maskedTenantKey, region);

        try {
            // 자격증명 암호화
            String encryptedAccessKeyId = encryptionService.encrypt(accessKeyId);
            String encryptedSecretAccessKey = encryptionService.encrypt(secretAccessKey);

            // UUID 기반 credentialKey 생성
            String credentialKey = UUID.randomUUID().toString();

            // CloudAccountCredential 엔티티 생성 및 저장
            CloudAccountCredential credential = CloudAccountCredential.builder()
                    .credentialKey(credentialKey)
                    .accessKeyIdEncrypted(encryptedAccessKeyId)
                    .secretAccessKeyEncrypted(encryptedSecretAccessKey)
                    .region(region)
                    .build();

            CloudAccountCredential savedCredential = credentialRepository.save(credential);

            log.info("[AwsCredentialManager] storeCredentials - success, credentialKey={}", credentialKey);
            return savedCredential;

        } catch (Exception e) {
            log.error("[AwsCredentialManager] storeCredentials - encryption failed", e);
            throw new BusinessException(
                    CredentialErrorCode.CREDENTIAL_ENCRYPTION_FAILED,
                    "자격증명 암호화에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 자격증명 키로 AWS 자격증명을 조회하고 복호화합니다.
     *
     * @param credentialKey 자격증명 키 (UUID)
     * @return AwsCredentials 복호화된 자격증명
     * @throws BusinessException 조회 또는 복호화 실패 시
     */
    @Transactional(readOnly = true)
    public AwsCredentials getCredentials(String credentialKey) {
        log.debug("[AwsCredentialManager] getCredentials - credentialKey={}", credentialKey);

        CloudAccountCredential credential = credentialRepository.findByCredentialKey(credentialKey)
                .orElseThrow(() -> new BusinessException(
                        CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                        "자격증명을 찾을 수 없습니다: " + credentialKey
                ));

        try {
            // 자격증명 복호화
            String accessKeyId = encryptionService.decrypt(credential.getAccessKeyIdEncrypted());
            String secretAccessKey = encryptionService.decrypt(credential.getSecretAccessKeyEncrypted());

            return AwsCredentials.builder()
                    .accessKeyId(accessKeyId)
                    .secretAccessKey(secretAccessKey)
                    .region(credential.getRegion())
                    .build();

        } catch (Exception e) {
            log.error("[AwsCredentialManager] getCredentials - decryption failed", e);
            throw new BusinessException(
                    CredentialErrorCode.CREDENTIAL_DECRYPTION_FAILED,
                    "자격증명 복호화에 실패했습니다: " + e.getMessage()
            );
        }
    }

    /**
     * 자격증명을 삭제합니다.
     *
     * @param credentialKey 자격증명 키 (UUID)
     * @throws BusinessException 삭제 실패 시
     */
    @Transactional
    public void deleteCredentials(String credentialKey) {
        log.info("[AwsCredentialManager] deleteCredentials - credentialKey={}", credentialKey);

        if (!credentialRepository.existsByCredentialKey(credentialKey)) {
            log.warn("[AwsCredentialManager] deleteCredentials - credential not found: {}", credentialKey);
            throw new BusinessException(
                    CredentialErrorCode.CREDENTIAL_NOT_FOUND,
                    "삭제할 자격증명을 찾을 수 없습니다: " + credentialKey
            );
        }

        credentialRepository.deleteByCredentialKey(credentialKey);
        log.info("[AwsCredentialManager] deleteCredentials - success");
    }

    /**
     * AWS 자격증명을 담는 내부 클래스
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AwsCredentials {
        @Masked(type = MaskingType.ACCESS_KEY)
        private String accessKeyId;
        
        @Masked(type = MaskingType.SECRET_KEY)
        private String secretAccessKey;
        
        private String region;
    }
}

