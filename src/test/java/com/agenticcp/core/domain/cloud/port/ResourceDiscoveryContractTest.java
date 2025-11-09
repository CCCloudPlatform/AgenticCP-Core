package com.agenticcp.core.domain.cloud.port;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceDiscoveryPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ResourceDiscoveryContractTest {

    protected abstract ResourceDiscoveryPort port();

    @Test
    void listResources_shouldNotThrow_andReturnPage() {
        Page<CloudResource> page = port().listResources(
                ResourceQuery.builder().page(0).size(10).build());
        assertThat(page).isNotNull();
    }
}
