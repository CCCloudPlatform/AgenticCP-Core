package com.agenticcp.core.domain.cloud.adapter.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsResourceDiscoveryAdapter;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsResourceMapper;
import com.agenticcp.core.domain.cloud.port.ResourceDiscoveryContractTest;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceDiscoveryPort;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "ENABLE_LOCALSTACK", matches = "true")
class AwsResourceDiscoveryAdapterIT extends ResourceDiscoveryContractTest {

    private final AwsResourceDiscoveryAdapter adapter = new AwsResourceDiscoveryAdapter(new AwsResourceMapper());

    @Override
    protected ResourceDiscoveryPort port() {
        return adapter;
    }
}
