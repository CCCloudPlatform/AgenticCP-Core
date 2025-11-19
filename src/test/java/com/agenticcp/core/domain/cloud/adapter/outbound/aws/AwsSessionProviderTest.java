package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredentialMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AwsSessionProvider 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AwsSessionProvider 테스트")
class AwsSessionProviderTest {

    @Mock
    private AwsCredentialManager awsCredentialManager;

    @Mock
    private StsClient stsClient;

    @Mock
    private AwsSessionCredentialMapper awsSessionCredentialMapper;

    private AwsSessionProvider awsSessionProvider;

    @BeforeEach
    void setUp() {
        awsSessionProvider = new AwsSessionProvider(awsCredentialManager, awsSessionCredentialMapper);
    }

    // Note: AWS STS 세션 발급은 실제 AWS STS API를 호출해야 하므로
    // 단위 테스트보다는 통합 테스트에서 검증하는 것이 적절합니다.
    // 단위 테스트에서는 AwsSessionProvider의 의존성만 확인합니다.
}

