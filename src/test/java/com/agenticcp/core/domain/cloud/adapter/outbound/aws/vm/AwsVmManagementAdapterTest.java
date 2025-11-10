package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

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
 * AwsVmManagementAdapter의 기본 기능을 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class AwsVmManagementAdapterTest {

    @Mock
    private Ec2Client vmClient;

    @Mock
    private AwsVmMapper mapper;

    private AwsVmManagementAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AwsVmManagementAdapter(vmClient, mapper);
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
        AwsVmManagementAdapter adapter = new AwsVmManagementAdapter(vmClient, mapper);

        // Then
        assertThat(adapter).isNotNull();
        assertThat(adapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }

    @Test
    void 어댑터_의존성_주입_테스트() {
        // Given
        Ec2Client mockClient = mock(Ec2Client.class);
        AwsVmMapper mockMapper = mock(AwsVmMapper.class);

        // When
        AwsVmManagementAdapter adapter = new AwsVmManagementAdapter(mockClient, mockMapper);

        // Then
        assertThat(adapter).isNotNull();
        assertThat(adapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }
}