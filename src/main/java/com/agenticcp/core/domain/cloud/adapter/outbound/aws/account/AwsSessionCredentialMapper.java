package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.model.GetSessionTokenResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * AWS STS 세션 자격증명을 CloudSessionCredential로 변환하는 Mapper.
 */
@Slf4j
@Component
public class AwsSessionCredentialMapper {

    public CloudSessionCredential toCloudSessionCredential(GetSessionTokenResponse response, String region) {
        if (response == null || response.credentials() == null) {
            log.warn("[AwsSessionCredentialMapper] Empty STS response provided");
            return null;
        }

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                response.credentials().expiration(),
                ZoneId.systemDefault()
        );

        return AwsSessionCredential.builder()
                .accessKeyId(response.credentials().accessKeyId())
                .secretAccessKey(response.credentials().secretAccessKey())
                .sessionToken(response.credentials().sessionToken())
                .region(region)
                .expiresAt(expiresAt)
                .build();
    }
}

