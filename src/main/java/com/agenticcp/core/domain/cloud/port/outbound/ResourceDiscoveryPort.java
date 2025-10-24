package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ResourceDiscoveryPort {
    Page<CloudResource> listResources(ResourceQuery query);
    Optional<CloudResource> getResource(ResourceIdentity id);
}
