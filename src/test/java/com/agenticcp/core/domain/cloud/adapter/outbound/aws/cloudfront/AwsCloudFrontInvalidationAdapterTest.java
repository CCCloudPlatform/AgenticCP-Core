package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsCloudFrontConfig;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.cdn.CreateInvalidationCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.InvalidationResult;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AwsCloudFrontInvalidationAdapter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AwsCloudFrontInvalidationAdapter 테스트")
class AwsCloudFrontInvalidationAdapterTest {

    @Mock
    private AwsCloudFrontConfig awsCloudFrontConfig;

    @Mock
    private AwsCloudFrontErrorTranslator errorTranslator;

    @Mock
    private AccountCredentialManagementPort accountCredentialManagementPort;

    @Mock
    private CloudFrontClient cloudFrontClient;

    @InjectMocks
    private AwsCloudFrontInvalidationAdapter adapter;

    private static final String TENANT_KEY = "test-tenant";
    private static final String ACCOUNT_SCOPE = "123456789012";
    private static final String DISTRIBUTION_ID = "E2QWRUHAPOMQZL";
    private static final String INVALIDATION_ID = "I2J3K4L5M6N7O";

    private AwsSessionCredential session;

    @BeforeEach
    void setUp() {
        session = AwsSessionCredential.builder()
                .accessKeyId("AKIA_TEST")
                .secretAccessKey("secret")
                .sessionToken("token")
                .region("us-east-1")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        // CloudFrontClient close()가 예외를 던지지 않도록 설정
        doNothing().when(cloudFrontClient).close();

        // errorTranslator 기본 모킹
        lenient().when(errorTranslator.translate(any(Exception.class)))
                .thenAnswer(invocation -> {
                    Exception e = invocation.getArgument(0);
                    return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE, e.getMessage());
                });

        // 공통 Mock 설정
        lenient().when(accountCredentialManagementPort.getSession(
                eq(TENANT_KEY), eq(ACCOUNT_SCOPE), eq(CloudProvider.ProviderType.AWS)))
                .thenReturn(session);
        lenient().when(awsCloudFrontConfig.createCloudFrontClient(session))
                .thenReturn(cloudFrontClient);
    }

    @Nested
    @DisplayName("캐시 무효화 생성 테스트")
    class CreateInvalidationTest {

        @Test
        @DisplayName("정상적인 무효화 생성 (CallerReference 제공)")
        void createInvalidation_Success_WithCallerReference() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/path1", "/path2"))
                    .callerReference("custom-reference-123")
                    .build();

            Instant createTime = Instant.now();
            Invalidation invalidation = Invalidation.builder()
                    .id(INVALIDATION_ID)
                    .status("InProgress")
                    .createTime(createTime)
                    .invalidationBatch(InvalidationBatch.builder()
                            .paths(Paths.builder()
                                    .quantity(2)
                                    .items(List.of("/path1", "/path2"))
                                    .build())
                            .callerReference("custom-reference-123")
                            .build())
                    .build();

            CreateInvalidationResponse response = CreateInvalidationResponse.builder()
                    .invalidation(invalidation)
                    .location("https://cloudfront.amazonaws.com/invalidation/" + INVALIDATION_ID)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.createInvalidation(any(CreateInvalidationRequest.class)))
                        .thenReturn(response);

                // When
                InvalidationResult result = adapter.createInvalidation(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.invalidationId()).isEqualTo(INVALIDATION_ID);
                assertThat(result.distributionId()).isEqualTo(DISTRIBUTION_ID);
                assertThat(result.status()).isEqualTo("InProgress");
                assertThat(result.paths()).containsExactly("/path1", "/path2");
                assertThat(result.createTime()).isNotNull();

                verify(cloudFrontClient).createInvalidation(any(CreateInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("정상적인 무효화 생성 (CallerReference 미제공, 자동 생성)")
        void createInvalidation_Success_WithoutCallerReference() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/*"))
                    .callerReference(null)
                    .build();

            Instant createTime = Instant.now();
            Invalidation invalidation = Invalidation.builder()
                    .id(INVALIDATION_ID)
                    .status("InProgress")
                    .createTime(createTime)
                    .invalidationBatch(InvalidationBatch.builder()
                            .paths(Paths.builder()
                                    .quantity(1)
                                    .items(List.of("/*"))
                                    .build())
                            .callerReference("auto-generated-reference")
                            .build())
                    .build();

            CreateInvalidationResponse response = CreateInvalidationResponse.builder()
                    .invalidation(invalidation)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.createInvalidation(any(CreateInvalidationRequest.class)))
                        .thenReturn(response);

                // When
                InvalidationResult result = adapter.createInvalidation(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.invalidationId()).isEqualTo(INVALIDATION_ID);
                assertThat(result.paths()).containsExactly("/*");

                verify(cloudFrontClient).createInvalidation(any(CreateInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("정상적인 무효화 생성 (경로 목록 비어있음, 기본값 사용)")
        void createInvalidation_Success_WithEmptyPaths() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of())
                    .callerReference("test-reference")
                    .build();

            Instant createTime = Instant.now();
            Invalidation invalidation = Invalidation.builder()
                    .id(INVALIDATION_ID)
                    .status("InProgress")
                    .createTime(createTime)
                    .invalidationBatch(InvalidationBatch.builder()
                            .paths(Paths.builder()
                                    .quantity(1)
                                    .items(List.of("/*"))
                                    .build())
                            .callerReference("test-reference")
                            .build())
                    .build();

            CreateInvalidationResponse response = CreateInvalidationResponse.builder()
                    .invalidation(invalidation)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.createInvalidation(any(CreateInvalidationRequest.class)))
                        .thenReturn(response);

                // When
                InvalidationResult result = adapter.createInvalidation(command);

                // Then
                assertThat(result).isNotNull();
                assertThat(result.paths()).containsExactly("/*"); // 기본값 사용

                verify(cloudFrontClient).createInvalidation(any(CreateInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("무효화 생성 실패 (AccountScope 없음)")
        void createInvalidation_WithoutAccountScope_ThrowsException() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(null)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/path1"))
                    .build();

            // When & Then
            assertThatThrownBy(() -> adapter.createInvalidation(command))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AccountScope가 필요합니다");

            verify(cloudFrontClient, never()).createInvalidation(any(CreateInvalidationRequest.class));
        }

        @Test
        @DisplayName("무효화 생성 실패 (AWS SDK 예외)")
        void createInvalidation_AwsSdkException_ThrowsBusinessException() {
            // Given
            CreateInvalidationCommand command = CreateInvalidationCommand.builder()
                    .accountScope(ACCOUNT_SCOPE)
                    .distributionId(DISTRIBUTION_ID)
                    .paths(List.of("/path1"))
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                doThrow(CloudFrontException.builder()
                        .message("Distribution not found")
                        .statusCode(404)
                        .build())
                        .when(cloudFrontClient).createInvalidation(any(CreateInvalidationRequest.class));

                BusinessException translatedException = new BusinessException(
                        CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND, "Distribution not found");
                when(errorTranslator.translate(any(CloudFrontException.class)))
                        .thenReturn(translatedException);

                // When & Then
                assertThatThrownBy(() -> adapter.createInvalidation(command))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("캐시 무효화 조회 테스트")
    class GetInvalidationTest {

        @Test
        @DisplayName("정상적인 무효화 조회")
        void getInvalidation_Success() {
            // Given
            Instant createTime = Instant.now();
            Invalidation invalidation = Invalidation.builder()
                    .id(INVALIDATION_ID)
                    .status("Completed")
                    .createTime(createTime)
                    .invalidationBatch(InvalidationBatch.builder()
                            .paths(Paths.builder()
                                    .quantity(2)
                                    .items(List.of("/path1", "/path2"))
                                    .build())
                            .callerReference("test-reference")
                            .build())
                    .build();

            GetInvalidationResponse response = GetInvalidationResponse.builder()
                    .invalidation(invalidation)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.getInvalidation(any(GetInvalidationRequest.class)))
                        .thenReturn(response);

                // When
                Optional<InvalidationResult> result = adapter.getInvalidation(
                        ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID);

                // Then
                assertThat(result).isPresent();
                InvalidationResult invalidationResult = result.get();
                assertThat(invalidationResult.invalidationId()).isEqualTo(INVALIDATION_ID);
                assertThat(invalidationResult.distributionId()).isEqualTo(DISTRIBUTION_ID);
                assertThat(invalidationResult.status()).isEqualTo("Completed");
                assertThat(invalidationResult.paths()).containsExactly("/path1", "/path2");
                assertThat(invalidationResult.createTime()).isNotNull();

                verify(cloudFrontClient).getInvalidation(any(GetInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("무효화 조회 실패 (존재하지 않음)")
        void getInvalidation_NotFound_ReturnsEmpty() {
            // Given
            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.getInvalidation(any(GetInvalidationRequest.class)))
                        .thenThrow(NoSuchInvalidationException.builder()
                                .message("Invalidation not found")
                                .build());

                // When
                Optional<InvalidationResult> result = adapter.getInvalidation(
                        ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID);

                // Then
                assertThat(result).isEmpty();

                verify(cloudFrontClient).getInvalidation(any(GetInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("무효화 조회 실패 (경로 정보 없음)")
        void getInvalidation_WithoutPaths_ReturnsEmptyPaths() {
            // Given
            Instant createTime = Instant.now();
            Invalidation invalidation = Invalidation.builder()
                    .id(INVALIDATION_ID)
                    .status("InProgress")
                    .createTime(createTime)
                    .invalidationBatch(InvalidationBatch.builder()
                            .paths((Paths) null)  // 경로 정보 없음
                            .build())
                    .build();

            GetInvalidationResponse response = GetInvalidationResponse.builder()
                    .invalidation(invalidation)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                when(cloudFrontClient.getInvalidation(any(GetInvalidationRequest.class)))
                        .thenReturn(response);

                // When
                Optional<InvalidationResult> result = adapter.getInvalidation(
                        ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID);

                // Then
                assertThat(result).isPresent();
                assertThat(result.get().paths()).isEmpty(); // 빈 리스트 반환

                verify(cloudFrontClient).getInvalidation(any(GetInvalidationRequest.class));
            }
        }

        @Test
        @DisplayName("무효화 조회 실패 (AccountScope 없음)")
        void getInvalidation_WithoutAccountScope_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> adapter.getInvalidation(null, DISTRIBUTION_ID, INVALIDATION_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AccountScope가 필요합니다");

            verify(cloudFrontClient, never()).getInvalidation(any(GetInvalidationRequest.class));
        }

        @Test
        @DisplayName("무효화 조회 실패 (AWS SDK 예외)")
        void getInvalidation_AwsSdkException_ThrowsBusinessException() {
            // Given
            try (MockedStatic<TenantContextHolder> mockedTenant = mockStatic(TenantContextHolder.class)) {
                mockedTenant.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenReturn(TENANT_KEY);

                doThrow(CloudFrontException.builder()
                        .message("Access denied")
                        .statusCode(403)
                        .build())
                        .when(cloudFrontClient).getInvalidation(any(GetInvalidationRequest.class));

                BusinessException translatedException = new BusinessException(
                        CloudErrorCode.PERMISSION_DENIED, "Access denied");
                when(errorTranslator.translate(any(CloudFrontException.class)))
                        .thenReturn(translatedException);

                // When & Then
                assertThatThrownBy(() -> adapter.getInvalidation(
                        ACCOUNT_SCOPE, DISTRIBUTION_ID, INVALIDATION_ID))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode")
                        .isEqualTo(CloudErrorCode.PERMISSION_DENIED);
            }
        }
    }

    @Nested
    @DisplayName("ProviderScoped 테스트")
    class ProviderScopedTest {

        @Test
        @DisplayName("getProviderType은 AWS를 반환")
        void getProviderType_ReturnsAWS() {
            // When
            CloudProvider.ProviderType result = adapter.getProviderType();

            // Then
            assertThat(result).isEqualTo(CloudProvider.ProviderType.AWS);
        }
    }
}

