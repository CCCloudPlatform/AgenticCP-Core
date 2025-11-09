package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceDiscoveryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AwsResourceDiscoveryAdapter implements ResourceDiscoveryPort, ProviderScoped {

    private final AwsResourceMapper mapper;

    @Override
    public Page<CloudResource> listResources(ResourceQuery query) {
        try {
            // TODO: call AWS SDK (EC2, etc.) - mapper will be used here
            return new PageImpl<>(List.of());
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Optional<CloudResource> getResource(ResourceIdentity id) {
        try {
            // TODO: call AWS SDK describe for id - mapper will be used here
            return Optional.empty();
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
