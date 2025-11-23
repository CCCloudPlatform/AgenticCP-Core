package com.agenticcp.core.domain.cloud.adapter.outbound.aws;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.outbound.ResourceLifecyclePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AWS 리소스 생명주기 어댑터
 * 
 * 세션 자격증명을 받아서 AWS SDK를 호출합니다.
 * 어댑터 내부에서 별도 자격증명을 조회하지 않습니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class AwsResourceLifecycleAdapter implements ResourceLifecyclePort, ProviderScoped {

    @Override
    public void start(ResourceIdentity id, CloudSessionCredential session) {
        try {
            // 세션 타입 검증
            if (!(session instanceof AwsSessionCredential awsSession)) {
                throw new IllegalArgumentException("AWS 세션이 필요합니다: " + session.getClass().getSimpleName());
            }
            
            log.debug("[AwsResourceLifecycleAdapter] start - resourceId={}, session expiresAt={}", 
                    id.getProviderResourceId(), awsSession.getExpiresAt());
            
            // TODO: AWS SDK를 사용하여 리소스 시작
            // awsSession.getAccessKeyId(), awsSession.getSecretAccessKey(), awsSession.getSessionToken() 사용
            // 내부에서 별도 자격증명 조회하지 않음
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void stop(ResourceIdentity id, CloudSessionCredential session) {
        try {
            // 세션 타입 검증
            if (!(session instanceof AwsSessionCredential awsSession)) {
                throw new IllegalArgumentException("AWS 세션이 필요합니다: " + session.getClass().getSimpleName());
            }
            
            log.debug("[AwsResourceLifecycleAdapter] stop - resourceId={}, session expiresAt={}", 
                    id.getProviderResourceId(), awsSession.getExpiresAt());
            
            // TODO: AWS SDK를 사용하여 리소스 중지
            // awsSession 사용, 내부에서 별도 자격증명 조회하지 않음
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void terminate(ResourceIdentity id, CloudSessionCredential session) {
        try {
            // 세션 타입 검증
            if (!(session instanceof AwsSessionCredential awsSession)) {
                throw new IllegalArgumentException("AWS 세션이 필요합니다: " + session.getClass().getSimpleName());
            }
            
            log.debug("[AwsResourceLifecycleAdapter] terminate - resourceId={}, session expiresAt={}", 
                    id.getProviderResourceId(), awsSession.getExpiresAt());
            
            // TODO: AWS SDK를 사용하여 리소스 종료
            // awsSession 사용, 내부에서 별도 자격증명 조회하지 않음
            
        } catch (Throwable t) {
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
