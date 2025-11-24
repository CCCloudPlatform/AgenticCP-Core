package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceTaggingPort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AwsResourceTaggingAdapter implements ResourceTaggingPort, ProviderScoped {
    @Override
    public void putTags(ResourceIdentity id, Map<String, String> tags) {
        try {
            // TODO: AWS SDK create/update tags
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Map<String, String> getTags(ResourceIdentity id) {
        try {
            // TODO: AWS SDK list tags
            return Map.of();
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
