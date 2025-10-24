package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceLifecyclePort;
import org.springframework.stereotype.Component;

@Component
public class AwsResourceLifecycleAdapter implements ResourceLifecyclePort, ProviderScoped {

    @Override
    public void start(ResourceIdentity id) {
        try {
            // TODO: AWS SDK start
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void stop(ResourceIdentity id) {
        try {
            // TODO: AWS SDK stop
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void terminate(ResourceIdentity id) {
        try {
            // TODO: AWS SDK terminate
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
