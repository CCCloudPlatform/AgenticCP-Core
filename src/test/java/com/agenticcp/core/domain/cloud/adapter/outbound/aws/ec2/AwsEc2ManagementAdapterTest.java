package com.agenticcp.core.domain.cloud.adapter.outbound.aws.ec2;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * AWS EC2 관리 어댑터 기본 테스트
 * 
 * AwsEc2ManagementAdapter의 기본 기능을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class AwsEc2ManagementAdapterTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private AwsEc2Mapper mapper;

    private AwsEc2ManagementAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AwsEc2ManagementAdapter(ec2Client, mapper);
    }

    @Test
    void getProviderType_테스트() {
        // When
        ProviderType result = adapter.getProviderType();

        // Then
        assertThat(result).isEqualTo(ProviderType.AWS);
    }

    @Test
    void 어댑터_인스턴스_생성_테스트() {
        // Given & When
        AwsEc2ManagementAdapter adapter = new AwsEc2ManagementAdapter(ec2Client, mapper);

        // Then
        assertThat(adapter).isNotNull();
        assertThat(adapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }

    @Test
    void 어댑터_의존성_주입_테스트() {
        // Given
        Ec2Client mockClient = mock(Ec2Client.class);
        AwsEc2Mapper mockMapper = mock(AwsEc2Mapper.class);

        // When
        AwsEc2ManagementAdapter adapter = new AwsEc2ManagementAdapter(mockClient, mockMapper);

        // Then
        assertThat(adapter).isNotNull();
        assertThat(adapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }
}