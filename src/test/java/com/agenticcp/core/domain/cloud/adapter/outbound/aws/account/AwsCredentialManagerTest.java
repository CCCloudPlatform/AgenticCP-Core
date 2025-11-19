package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsCredentialManager;
import com.agenticcp.core.domain.cloud.entity.CloudAccountCredential;
import com.agenticcp.core.domain.cloud.exception.CredentialErrorCode;
import com.agenticcp.core.domain.cloud.repository.CloudAccountCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * AwsCredentialManager 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsCredentialManager 단위 테스트")
class AwsCredentialManagerTest {

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private CloudAccountCredentialRepository credentialRepository;

    @InjectMocks
    private AwsCredentialManager awsCredentialManager;

    private String tenantKey;
    private String accessKeyId;
    private String secretAccessKey;
    private String region;
    private String encryptedAccessKeyId;
    private String encryptedSecretAccessKey;

    @BeforeEach
    void setUp() {
        tenantKey = "test-tenant";
        accessKeyId = "AKIAIOSFODNN7EXAMPLE";
        secretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
        region = "us-east-1";
        encryptedAccessKeyId = "encrypted-access-key-id";
        encryptedSecretAccessKey = "encrypted-secret-access-key";
    }

    @Nested
    @DisplayName("자격증명 저장 테스트")
    class StoreCredentialsTest {

        @Test
        @DisplayName("자격증명 암호화 및 저장 성공")
        void storeCredentials_Success() {
            // given
            given(encryptionService.encrypt(accessKeyId)).willReturn(encryptedAccessKeyId);
            given(encryptionService.encrypt(secretAccessKey)).willReturn(encryptedSecretAccessKey);

            CloudAccountCredential savedCredential = CloudAccountCredential.builder()
                    .credentialKey("test-credential-key")
                    .accessKeyIdEncrypted(encryptedAccessKeyId)
                    .secretAccessKeyEncrypted(encryptedSecretAccessKey)
                    .region(region)
                    .build();
            savedCredential.setId(1L);

            given(credentialRepository.save(any(CloudAccountCredential.class)))
                    .willReturn(savedCredential);

            // when
            CloudAccountCredential result = awsCredentialManager.storeCredentials(
                    tenantKey, accessKeyId, secretAccessKey, region);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCredentialKey()).isEqualTo("test-credential-key");
            assertThat(result.getAccessKeyIdEncrypted()).isEqualTo(encryptedAccessKeyId);
            assertThat(result.getSecretAccessKeyEncrypted()).isEqualTo(encryptedSecretAccessKey);
            assertThat(result.getRegion()).isEqualTo(region);

            // verify
            then(encryptionService).should(times(1)).encrypt(accessKeyId);
            then(encryptionService).should(times(1)).encrypt(secretAccessKey);

            ArgumentCaptor<CloudAccountCredential> credentialCaptor = 
                    ArgumentCaptor.forClass(CloudAccountCredential.class);
            then(credentialRepository).should(times(1)).save(credentialCaptor.capture());

            CloudAccountCredential capturedCredential = credentialCaptor.getValue();
            assertThat(capturedCredential.getAccessKeyIdEncrypted()).isEqualTo(encryptedAccessKeyId);
            assertThat(capturedCredential.getSecretAccessKeyEncrypted()).isEqualTo(encryptedSecretAccessKey);
            assertThat(capturedCredential.getRegion()).isEqualTo(region);
            assertThat(capturedCredential.getCredentialKey()).isNotNull();
        }

        @Test
        @DisplayName("암호화 실패 시 BusinessException 발생")
        void storeCredentials_EncryptionFails_ThrowsException() {
            // given
            given(encryptionService.encrypt(accessKeyId))
                    .willThrow(new RuntimeException("Encryption failed"));

            // when & then
            assertThatThrownBy(() -> awsCredentialManager.storeCredentials(
                    tenantKey, accessKeyId, secretAccessKey, region))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자격증명 암호화에 실패했습니다")
                    .extracting("errorCode")
                    .isEqualTo(CredentialErrorCode.CREDENTIAL_ENCRYPTION_FAILED);

            // verify
            then(credentialRepository).should(times(0)).save(any(CloudAccountCredential.class));
        }

        @Test
        @DisplayName("저장 실패 시 BusinessException 발생")
        void storeCredentials_SaveFails_ThrowsException() {
            // given
            given(encryptionService.encrypt(accessKeyId)).willReturn(encryptedAccessKeyId);
            given(encryptionService.encrypt(secretAccessKey)).willReturn(encryptedSecretAccessKey);
            given(credentialRepository.save(any(CloudAccountCredential.class)))
                    .willThrow(new RuntimeException("Database error"));

            // when & then
            assertThatThrownBy(() -> awsCredentialManager.storeCredentials(
                    tenantKey, accessKeyId, secretAccessKey, region))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자격증명 암호화에 실패했습니다")
                    .extracting("errorCode")
                    .isEqualTo(CredentialErrorCode.CREDENTIAL_ENCRYPTION_FAILED);
        }
    }

    @Nested
    @DisplayName("자격증명 조회 테스트")
    class GetCredentialsTest {

        @Test
        @DisplayName("자격증명 복호화 및 조회 성공")
        void getCredentials_Success() {
            // given
            String credentialKey = "test-credential-key";
            CloudAccountCredential credential = CloudAccountCredential.builder()
                    .credentialKey(credentialKey)
                    .accessKeyIdEncrypted(encryptedAccessKeyId)
                    .secretAccessKeyEncrypted(encryptedSecretAccessKey)
                    .region(region)
                    .build();
            credential.setId(1L);

            given(credentialRepository.findByCredentialKey(credentialKey))
                    .willReturn(Optional.of(credential));
            given(encryptionService.decrypt(encryptedAccessKeyId)).willReturn(accessKeyId);
            given(encryptionService.decrypt(encryptedSecretAccessKey)).willReturn(secretAccessKey);

            // when
            AwsCredentialManager.AwsCredentials result = 
                    awsCredentialManager.getCredentials(credentialKey);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessKeyId()).isEqualTo(accessKeyId);
            assertThat(result.getSecretAccessKey()).isEqualTo(secretAccessKey);

            // verify
            then(credentialRepository).should(times(1)).findByCredentialKey(credentialKey);
            then(encryptionService).should(times(1)).decrypt(encryptedAccessKeyId);
            then(encryptionService).should(times(1)).decrypt(encryptedSecretAccessKey);
        }

        @Test
        @DisplayName("존재하지 않는 자격증명 조회 시 BusinessException 발생")
        void getCredentials_NotFound_ThrowsException() {
            // given
            String credentialKey = "non-existent-key";
            given(credentialRepository.findByCredentialKey(credentialKey))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> awsCredentialManager.getCredentials(credentialKey))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자격증명을 찾을 수 없습니다")
                    .extracting("errorCode")
                    .isEqualTo(CredentialErrorCode.CREDENTIAL_NOT_FOUND);

            // verify
            then(encryptionService).should(times(0)).decrypt(anyString());
        }

        @Test
        @DisplayName("복호화 실패 시 BusinessException 발생")
        void getCredentials_DecryptionFails_ThrowsException() {
            // given
            String credentialKey = "test-credential-key";
            CloudAccountCredential credential = CloudAccountCredential.builder()
                    .credentialKey(credentialKey)
                    .accessKeyIdEncrypted(encryptedAccessKeyId)
                    .secretAccessKeyEncrypted(encryptedSecretAccessKey)
                    .region(region)
                    .build();
            credential.setId(1L);

            given(credentialRepository.findByCredentialKey(credentialKey))
                    .willReturn(Optional.of(credential));
            given(encryptionService.decrypt(encryptedAccessKeyId))
                    .willThrow(new RuntimeException("Decryption failed"));

            // when & then
            assertThatThrownBy(() -> awsCredentialManager.getCredentials(credentialKey))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("자격증명 복호화에 실패했습니다")
                    .extracting("errorCode")
                    .isEqualTo(CredentialErrorCode.CREDENTIAL_DECRYPTION_FAILED);
        }
    }

    @Nested
    @DisplayName("자격증명 삭제 테스트")
    class DeleteCredentialsTest {

        @Test
        @DisplayName("자격증명 삭제 성공")
        void deleteCredentials_Success() {
            // given
            String credentialKey = "test-credential-key";
            given(credentialRepository.existsByCredentialKey(credentialKey)).willReturn(true);

            // when
            awsCredentialManager.deleteCredentials(credentialKey);

            // then
            then(credentialRepository).should(times(1)).existsByCredentialKey(credentialKey);
            then(credentialRepository).should(times(1)).deleteByCredentialKey(credentialKey);
        }

        @Test
        @DisplayName("존재하지 않는 자격증명 삭제 시 BusinessException 발생")
        void deleteCredentials_NotFound_ThrowsException() {
            // given
            String credentialKey = "non-existent-key";
            given(credentialRepository.existsByCredentialKey(credentialKey)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> awsCredentialManager.deleteCredentials(credentialKey))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("삭제할 자격증명을 찾을 수 없습니다")
                    .extracting("errorCode")
                    .isEqualTo(CredentialErrorCode.CREDENTIAL_NOT_FOUND);

            // verify
            then(credentialRepository).should(times(0)).deleteByCredentialKey(anyString());
        }
    }
}

