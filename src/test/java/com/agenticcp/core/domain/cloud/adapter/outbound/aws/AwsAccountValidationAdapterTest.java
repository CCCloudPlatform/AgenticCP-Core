package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.port.model.account.AccountValidationRequest;
import com.agenticcp.core.domain.cloud.port.model.account.AccountValidationResult;
import com.agenticcp.core.domain.cloud.port.model.account.ConnectionTestResult;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * AwsAccountValidationAdapter 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsAccountValidationAdapter 단위 테스트")
class AwsAccountValidationAdapterTest {

    @Mock
    private AwsClientConfig awsClientConfig;

    @InjectMocks
    private AwsAccountValidationAdapter adapter;

    private AccountValidationRequest validationRequest;

    @BeforeEach
    void setUp() {
        validationRequest = AccountValidationRequest.builder()
                .providerType(ProviderType.AWS)
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .secretAccessKey("wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
                .region("us-east-1")
                .build();
    }

    @Nested
    @DisplayName("계정 검증 테스트")
    class ValidateAccountTest {

        @Test
        @DisplayName("AWS 계정 검증 성공")
        void validateAccount_Success() {
            // given
            StsClient stsClient = mock(StsClient.class);
            GetCallerIdentityResponse stsResponse = GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .arn("arn:aws:iam::123456789012:user/test-user")
                    .userId("AIDAI23HXS4EXAMPLE")
                    .build();

            given(awsClientConfig.createStsClient(
                    validationRequest.getAccessKeyId(),
                    validationRequest.getSecretAccessKey(),
                    validationRequest.getRegion()))
                    .willReturn(stsClient);
            given(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .willReturn(stsResponse);

            // when
            AccountValidationResult result = adapter.validateAccount(validationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getValid()).isTrue();
            assertThat(result.getAccountScope()).isEqualTo("123456789012");
            assertThat(result.getRegion()).isEqualTo("us-east-1");
            assertThat(result.getMessage()).contains("AWS 계정 검증 성공");
            assertThat(result.getMetadata()).containsEntry("arn", "arn:aws:iam::123456789012:user/test-user");
            assertThat(result.getMetadata()).containsEntry("userId", "AIDAI23HXS4EXAMPLE");

            then(awsClientConfig).should(times(1)).createStsClient(anyString(), anyString(), anyString());
            then(stsClient).should(times(1)).getCallerIdentity(any(GetCallerIdentityRequest.class));
        }

        @Test
        @DisplayName("잘못된 자격증명으로 검증 실패")
        void validateAccount_InvalidCredentials_ReturnsInvalidResult() {
            // given
            StsClient stsClient = mock(StsClient.class);

            given(awsClientConfig.createStsClient(
                    validationRequest.getAccessKeyId(),
                    validationRequest.getSecretAccessKey(),
                    validationRequest.getRegion()))
                    .willReturn(stsClient);
            given(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .willThrow(StsException.builder()
                            .message("The security token included in the request is invalid")
                            .build());

            // when
            AccountValidationResult result = adapter.validateAccount(validationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getValid()).isFalse();
            assertThat(result.getMessage()).contains("AWS 계정 검증 실패");
        }

        @Test
        @DisplayName("리전 정보가 없는 경우 기본 리전 사용")
        void validateAccount_NoRegion_UsesDefault() {
            // given
            validationRequest.setRegion(null);
            StsClient stsClient = mock(StsClient.class);
            GetCallerIdentityResponse stsResponse = GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .arn("arn:aws:iam::123456789012:user/test-user")
                    .userId("AIDAI23HXS4EXAMPLE")
                    .build();

            given(awsClientConfig.createStsClient(anyString(), anyString(), anyString()))
                    .willReturn(stsClient);
            given(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .willReturn(stsResponse);

            // when
            AccountValidationResult result = adapter.validateAccount(validationRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getValid()).isTrue();
            assertThat(result.getRegion()).isEqualTo("us-east-1"); // 기본 리전
        }
    }

    @Nested
    @DisplayName("연결 테스트")
    class TestConnectionTest {

        @Test
        @DisplayName("AWS 연결 테스트 성공")
        void testConnection_Success() {
            // given
            Long accountId = 1L;
            Map<String, String> credentials = Map.of(
                    "accessKeyId", "AKIAIOSFODNN7EXAMPLE",
                    "secretAccessKey", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
                    "region", "us-east-1"
            );

            StsClient stsClient = mock(StsClient.class);
            GetCallerIdentityResponse stsResponse = GetCallerIdentityResponse.builder()
                    .account("123456789012")
                    .arn("arn:aws:iam::123456789012:user/test")
                    .build();

            given(awsClientConfig.createStsClient(
                    credentials.get("accessKeyId"),
                    credentials.get("secretAccessKey"),
                    credentials.get("region")))
                    .willReturn(stsClient);
            given(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .willReturn(stsResponse);

            // when
            ConnectionTestResult result = adapter.testConnection(accountId, credentials);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getMessage()).contains("AWS 계정 연결 성공");
            assertThat(result.getTestedAt()).isNotNull();

            then(awsClientConfig).should(times(1)).createStsClient(anyString(), anyString(), anyString());
            then(stsClient).should(times(1)).getCallerIdentity(any(GetCallerIdentityRequest.class));
        }

        @Test
        @DisplayName("연결 테스트 실패")
        void testConnection_Failed_ReturnsFailureResult() {
            // given
            Long accountId = 1L;
            Map<String, String> credentials = Map.of(
                    "accessKeyId", "INVALID_KEY",
                    "secretAccessKey", "INVALID_SECRET",
                    "region", "us-east-1"
            );

            StsClient stsClient = mock(StsClient.class);

            given(awsClientConfig.createStsClient(anyString(), anyString(), anyString()))
                    .willReturn(stsClient);
            given(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class)))
                    .willThrow(StsException.builder()
                            .message("Invalid credentials")
                            .build());

            // when
            ConnectionTestResult result = adapter.testConnection(accountId, credentials);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getMessage()).contains("AWS 계정 연결 실패");
            assertThat(result.getAccountId()).isEqualTo(accountId);
        }
    }

}


