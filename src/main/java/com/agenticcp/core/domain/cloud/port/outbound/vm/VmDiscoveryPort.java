package com.agenticcp.core.domain.cloud.port.outbound.vm;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * VM 조회/탐색 책임 포트
 */
public interface VmDiscoveryPort {

    Page<CloudResource> listInstances(VmQuery query);

    Optional<CloudResource> getInstance(String instanceId);

    String getInstanceStatus(String instanceId);

    boolean waitForInstanceStatus(String instanceId, String targetStatus, int timeoutSeconds);
}

