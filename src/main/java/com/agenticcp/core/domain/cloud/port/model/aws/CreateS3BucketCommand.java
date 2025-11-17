package com.agenticcp.core.domain.cloud.port.model.aws;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CreateS3BucketCommand {
    private final String bucketName;
    private final String region;
    private final Map<String, String> tags;
    private final String objectOwnership;
    private final Boolean objectLockEnabled;
}
