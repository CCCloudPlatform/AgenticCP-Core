package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AwsVmAdapterProviderTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private AwsVmMapper mapper;

    @Mock
    private AwsClientConfig awsClientConfig;

    @Mock
    private AccountCredentialManagementPort credentialPort;

    private AwsVmDiscoveryAdapter discoveryAdapter;
    private AwsVmLifecycleAdapter lifecycleAdapter;
    private AwsVmTaggingAdapter taggingAdapter;

    @BeforeEach
    void setUp() {
        discoveryAdapter = new AwsVmDiscoveryAdapter(awsClientConfig, credentialPort, mapper);
        lifecycleAdapter = new AwsVmLifecycleAdapter(awsClientConfig, mapper);
        taggingAdapter = new AwsVmTaggingAdapter(ec2Client);
    }

    @Test
    void discoveryAdapter_providerType() {
        assertThat(discoveryAdapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }

    @Test
    void lifecycleAdapter_providerType() {
        assertThat(lifecycleAdapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }

    @Test
    void taggingAdapter_providerType() {
        assertThat(taggingAdapter.getProviderType()).isEqualTo(ProviderType.AWS);
    }
}

