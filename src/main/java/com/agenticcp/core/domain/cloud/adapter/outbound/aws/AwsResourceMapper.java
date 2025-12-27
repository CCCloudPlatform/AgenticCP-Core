package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import org.springframework.stereotype.Component;

@Component
public class AwsResourceMapper {
    public CloudResource toCloudResource(Object awsDescribeInstanceResult) {
        // Canonical 매핑 스켈레톤: 필수만 매핑, 나머지는 configuration/metadata에 보존
        return CloudResource.builder()
                .resourceId("i-unknown")
                .name("unknown")
                .provider("AWS")
                .region("us-east-1")
                .type("INSTANCE")
                .build();
    }
}
