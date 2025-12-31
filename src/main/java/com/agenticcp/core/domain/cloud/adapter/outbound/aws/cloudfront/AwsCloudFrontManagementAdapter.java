package com.agenticcp.core.domain.cloud.adapter.outbound.aws.cloudfront;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsCloudFrontConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.cdn.*;
import com.agenticcp.core.domain.cloud.port.outbound.cdn.CDNManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * AWS CloudFront Distribution 관리 어댑터
 * 
 * AWS CloudFront SDK를 사용하여 Distribution 생성/수정/삭제 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsCloudFrontManagementAdapter implements CDNManagementPort, ProviderScoped {

    private final AwsCloudFrontConfig awsCloudFrontConfig;
    private final AwsCloudFrontMapper awsCloudFrontMapper;

    @Override
    public CloudResource createDistribution(CreateDistributionCommand command) {
        try (CloudFrontClient cloudFrontClient = awsCloudFrontConfig.createCloudFrontClient(command.session())) {
            
            // DistributionConfig 생성
            DistributionConfig distributionConfig = buildDistributionConfig(command);
            
            // Distribution 생성 요청
            CreateDistributionRequest request = CreateDistributionRequest.builder()
                .distributionConfig(distributionConfig)
                .build();
            
            CreateDistributionResponse response = cloudFrontClient.createDistribution(request);
            
            // 태그 추가 (Distribution 생성 직후)
            if (command.tags() != null && !command.tags().isEmpty()) {
                try {
                    List<Tag> tags = command.tags().entrySet().stream()
                        .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                        .toList();
                    
                    Tags cfTags = Tags.builder()
                        .items(tags)
                        .build();
                    
                    TagResourceRequest tagRequest = TagResourceRequest.builder()
                        .resource(response.distribution().arn())
                        .tags(cfTags)
                        .build();
                    
                    cloudFrontClient.tagResource(tagRequest);
                    
                    log.info("[AwsCloudFrontManagementAdapter] Tags added to distribution: {}", response.distribution().id());
                } catch (Exception e) {
                    log.warn("[AwsCloudFrontManagementAdapter] Failed to add tags to distribution: {}", e.getMessage());
                }
            }
            
            return awsCloudFrontMapper.toCloudResource(
                response.distribution(), 
                response.eTag(), 
                command
            );
            
        } catch (Throwable e) {
            log.error("[AwsCloudFrontManagementAdapter] Failed to create distribution: {}", e.getMessage(), e);
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public CloudResource updateDistribution(UpdateDistributionCommand command) {
        try (CloudFrontClient cloudFrontClient = awsCloudFrontConfig.createCloudFrontClient(command.session())) {
            
            // 기존 Distribution 설정 조회
            GetDistributionRequest getRequest = GetDistributionRequest.builder()
                .id(command.distributionId())
                .build();
            
            GetDistributionResponse getResponse = cloudFrontClient.getDistribution(getRequest);
            DistributionConfig currentConfig = getResponse.distribution().distributionConfig();
            
            // 설정 업데이트
            DistributionConfig.Builder configBuilder = currentConfig.toBuilder();
            
            if (command.comment() != null) {
                configBuilder.comment(command.comment());
            }
            
            if (command.enabled() != null) {
                configBuilder.enabled(command.enabled());
            }
            
            // CacheBehavior 업데이트
            if (command.cacheBehaviors() != null && !command.cacheBehaviors().isEmpty()) {
                // 기본 CacheBehavior 설정
                CacheBehaviorConfig defaultBehavior = command.cacheBehaviors().get(0);
                DefaultCacheBehavior updatedDefaultBehavior = buildDefaultCacheBehavior(
                    defaultBehavior,
                    currentConfig.origins().items().get(0).id()
                );
                configBuilder.defaultCacheBehavior(updatedDefaultBehavior);
            }
            
            // Aliases 업데이트
            if (command.aliases() != null) {
                Aliases aliases = Aliases.builder()
                    .quantity(command.aliases().size())
                    .items(command.aliases())
                    .build();
                configBuilder.aliases(aliases);
            }
            
            // Distribution 업데이트 요청
            UpdateDistributionRequest updateRequest = UpdateDistributionRequest.builder()
                .id(command.distributionId())
                .distributionConfig(configBuilder.build())
                .ifMatch(command.etag())
                .build();
            
            UpdateDistributionResponse updateResponse = cloudFrontClient.updateDistribution(updateRequest);
            
            // 태그 업데이트 (기존 태그 삭제 후 새로 추가)
            if (command.tags() != null && !command.tags().isEmpty()) {
                try {
                    List<Tag> tags = command.tags().entrySet().stream()
                        .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                        .toList();
                    
                    Tags cfTags = Tags.builder()
                        .items(tags)
                        .build();
                    
                    TagResourceRequest tagRequest = TagResourceRequest.builder()
                        .resource(updateResponse.distribution().arn())
                        .tags(cfTags)
                        .build();
                    
                    cloudFrontClient.tagResource(tagRequest);
                    
                    log.info("[AwsCloudFrontManagementAdapter] Tags updated for distribution: {}", command.distributionId());
                } catch (Exception e) {
                    log.warn("[AwsCloudFrontManagementAdapter] Failed to update tags: {}", e.getMessage());
                }
            }
            
            return awsCloudFrontMapper.toCloudResource(
                updateResponse.distribution(),
                updateResponse.eTag(),
                command
            );
            
        } catch (Throwable e) {
            log.error("[AwsCloudFrontManagementAdapter] Failed to update distribution: {}", e.getMessage(), e);
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public void deleteDistribution(DeleteDistributionCommand command) {
        try (CloudFrontClient cloudFrontClient = awsCloudFrontConfig.createCloudFrontClient(command.session())) {
            
            // Distribution이 활성화되어 있다면 먼저 비활성화 필요
            GetDistributionRequest getRequest = GetDistributionRequest.builder()
                .id(command.distributionId())
                .build();
            
            GetDistributionResponse getResponse = cloudFrontClient.getDistribution(getRequest);
            
            if (getResponse.distribution().distributionConfig().enabled()) {
                log.info("[AwsCloudFrontManagementAdapter] Disabling distribution before deletion: {}", command.distributionId());
                
                DistributionConfig disabledConfig = getResponse.distribution()
                    .distributionConfig()
                    .toBuilder()
                    .enabled(false)
                    .build();
                
                UpdateDistributionRequest updateRequest = UpdateDistributionRequest.builder()
                    .id(command.distributionId())
                    .distributionConfig(disabledConfig)
                    .ifMatch(getResponse.eTag())
                    .build();
                
                UpdateDistributionResponse updateResponse = cloudFrontClient.updateDistribution(updateRequest);
                
                log.info("[AwsCloudFrontManagementAdapter] Distribution disabled, waiting for deployment to complete...");
                
                // Distribution이 Deployed 상태가 될 때까지 대기
                String finalETag = waitForDeployment(cloudFrontClient, command.distributionId(), updateResponse.eTag());
                
                // 삭제 요청 (배포 완료 후 최신 ETag 사용)
                DeleteDistributionRequest deleteRequest = DeleteDistributionRequest.builder()
                    .id(command.distributionId())
                    .ifMatch(finalETag)
                    .build();
                
                cloudFrontClient.deleteDistribution(deleteRequest);
                
            } else {
                // 이미 비활성화된 경우 바로 삭제
                DeleteDistributionRequest deleteRequest = DeleteDistributionRequest.builder()
                    .id(command.distributionId())
                    .ifMatch(command.etag())
                    .build();
                
                cloudFrontClient.deleteDistribution(deleteRequest);
            }
            
            log.info("[AwsCloudFrontManagementAdapter] Distribution deleted successfully: {}", command.distributionId());
            
        } catch (Throwable e) {
            log.error("[AwsCloudFrontManagementAdapter] Failed to delete distribution: {}", e.getMessage(), e);
            throw CloudErrorTranslator.translate(e);
        }
    }

    /**
     * CreateDistributionCommand로부터 DistributionConfig 생성
     */
    private DistributionConfig buildDistributionConfig(CreateDistributionCommand command) {
        // CallerReference (고유 식별자)
        String callerReference = UUID.randomUUID().toString();
        
        // Origins 설정
        Origins origins = buildOrigins(command.origin());
        
        // DefaultCacheBehavior 설정
        DefaultCacheBehavior defaultCacheBehavior = buildDefaultCacheBehavior(
            command.cacheBehaviors() != null && !command.cacheBehaviors().isEmpty() 
                ? command.cacheBehaviors().get(0)
                : createDefaultCacheBehavior(),
            command.origin().id()
        );
        
        // Aliases 설정 (CNAME)
        Aliases aliases = null;
        if (command.aliases() != null && !command.aliases().isEmpty()) {
            aliases = Aliases.builder()
                .quantity(command.aliases().size())
                .items(command.aliases())
                .build();
        }
        
        // ViewerCertificate 설정 (SSL/TLS)
        ViewerCertificate viewerCertificate = buildViewerCertificate(command.sslCertificateId());
        
        return DistributionConfig.builder()
            .callerReference(callerReference)
            .origins(origins)
            .defaultCacheBehavior(defaultCacheBehavior)
            .comment(command.comment() != null ? command.comment() : "")
            .enabled(command.enabled() != null ? command.enabled() : true)
            .aliases(aliases)
            .viewerCertificate(viewerCertificate)
            .build();
    }
    
    /**
     * OriginConfig로부터 Origins 생성
     */
    private Origins buildOrigins(OriginConfig originConfig) {
        Origin.Builder originBuilder = Origin.builder()
            .id(originConfig.id())
            .domainName(originConfig.domainName());
        
        if (originConfig.type() == OriginConfig.OriginType.PUBLIC_S3) {
            // S3 Origin 설정
            originBuilder.s3OriginConfig(S3OriginConfig.builder()
                .originAccessIdentity("")  // OAI 사용 안 함 (Phase 1)
                .build());
        } else {
            // Custom Origin 설정 (ELB, EC2 등)
            CustomOriginConfig customOriginConfig = CustomOriginConfig.builder()
                .httpPort(originConfig.httpPort() != null ? originConfig.httpPort() : 80)
                .httpsPort(originConfig.httpsPort() != null ? originConfig.httpsPort() : 443)
                .originProtocolPolicy(
                    originConfig.originProtocolPolicy() != null 
                        ? OriginProtocolPolicy.fromValue(originConfig.originProtocolPolicy())
                        : OriginProtocolPolicy.HTTPS_ONLY
                )
                .build();
            
            originBuilder.customOriginConfig(customOriginConfig);
        }
        
        return Origins.builder()
            .quantity(1)
            .items(originBuilder.build())
            .build();
    }
    
    /**
     * CacheBehaviorConfig로부터 DefaultCacheBehavior 생성
     */
    private DefaultCacheBehavior buildDefaultCacheBehavior(CacheBehaviorConfig config, String targetOriginId) {
        return DefaultCacheBehavior.builder()
            .targetOriginId(targetOriginId)
            .viewerProtocolPolicy(
                config.viewerProtocolPolicy() != null
                    ? ViewerProtocolPolicy.fromValue(config.viewerProtocolPolicy())
                    : ViewerProtocolPolicy.REDIRECT_TO_HTTPS
            )
            .allowedMethods(buildAllowedMethods(config.allowedMethods()))
            .compress(config.compress() != null ? config.compress() : true)
            .minTTL(0L)
            .defaultTTL(config.ttl() != null ? config.ttl() : 86400L)
            .maxTTL(31536000L)
            .forwardedValues(ForwardedValues.builder()
                .queryString(false)
                .cookies(CookiePreference.builder()
                    .forward(ItemSelection.NONE)
                    .build())
                .build())
            .trustedSigners(TrustedSigners.builder()
                .enabled(false)
                .quantity(0)
                .build())
            .build();
    }
    
    /**
     * 기본 CacheBehaviorConfig 생성
     */
    private CacheBehaviorConfig createDefaultCacheBehavior() {
        return CacheBehaviorConfig.builder()
            .pathPattern("/*")
            .ttl(86400L)  // 24시간
            .allowedMethods(List.of("GET", "HEAD", "OPTIONS"))
            .compress(true)
            .viewerProtocolPolicy("redirect-to-https")
            .build();
    }
    
    /**
     * AllowedMethods 생성
     */
    private AllowedMethods buildAllowedMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            // 기본: GET, HEAD
            return AllowedMethods.builder()
                .quantity(2)
                .items(Method.GET, Method.HEAD)
                .cachedMethods(CachedMethods.builder()
                    .quantity(2)
                    .items(Method.GET, Method.HEAD)
                    .build())
                .build();
        }
        
        List<Method> awsMethods = new ArrayList<>();
        for (String method : methods) {
            awsMethods.add(Method.fromValue(method));
        }
        
        return AllowedMethods.builder()
            .quantity(awsMethods.size())
            .items(awsMethods)
            .cachedMethods(CachedMethods.builder()
                .quantity(Math.min(2, awsMethods.size()))
                .items(Method.GET, Method.HEAD)
                .build())
            .build();
    }
    
    /**
     * ViewerCertificate 생성
     */
    private ViewerCertificate buildViewerCertificate(String sslCertificateId) {
        if (sslCertificateId != null && !sslCertificateId.isEmpty()) {
            // ACM 인증서 사용
            return ViewerCertificate.builder()
                .acmCertificateArn(sslCertificateId)
                .sslSupportMethod(SSLSupportMethod.SNI_ONLY)
                .minimumProtocolVersion(MinimumProtocolVersion.TLS_V1_2_2021)
                .build();
        } else {
            // 기본 CloudFront 인증서 사용
            return ViewerCertificate.builder()
                .cloudFrontDefaultCertificate(true)
                .build();
        }
    }

    /**
     * Distribution이 Deployed 상태가 될 때까지 대기
     * 
     * @param cloudFrontClient CloudFront 클라이언트
     * @param distributionId Distribution ID
     * @param initialETag 초기 ETag
     * @return 배포 완료 후 최신 ETag
     */
    private String waitForDeployment(CloudFrontClient cloudFrontClient, String distributionId, String initialETag) {
        int maxWaitMinutes = 10; // 최대 10분 대기
        int pollIntervalSeconds = 10; // 10초마다 상태 확인
        int maxAttempts = (maxWaitMinutes * 60) / pollIntervalSeconds;
        
        String currentETag = initialETag;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                GetDistributionRequest getRequest = GetDistributionRequest.builder()
                    .id(distributionId)
                    .build();
                
                GetDistributionResponse getResponse = cloudFrontClient.getDistribution(getRequest);
                String status = getResponse.distribution().status();
                currentETag = getResponse.eTag();
                
                log.debug("[AwsCloudFrontManagementAdapter] Distribution status check (attempt {}/{}): status={}, distributionId={}", 
                    attempt, maxAttempts, status, distributionId);
                
                if ("Deployed".equals(status)) {
                    log.info("[AwsCloudFrontManagementAdapter] Distribution deployment completed: distributionId={}", distributionId);
                    return currentETag;
                }
                
                // InProgress 상태면 계속 대기
                if ("InProgress".equals(status)) {
                    try {
                        TimeUnit.SECONDS.sleep(pollIntervalSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("[AwsCloudFrontManagementAdapter] Interrupted while waiting for deployment: {}", distributionId);
                        throw new RuntimeException("Deployment wait interrupted", e);
                    }
                    continue;
                }
                
                // 기타 상태면 경고 로그만 남기고 계속 시도
                log.warn("[AwsCloudFrontManagementAdapter] Unexpected distribution status: status={}, distributionId={}", 
                    status, distributionId);
                
            } catch (Exception e) {
                log.warn("[AwsCloudFrontManagementAdapter] Error checking distribution status (attempt {}/{}): {}", 
                    attempt, maxAttempts, e.getMessage());
                
                // 마지막 시도가 아니면 계속 진행
                if (attempt < maxAttempts) {
                    try {
                        TimeUnit.SECONDS.sleep(pollIntervalSeconds);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Deployment wait interrupted", ie);
                    }
                    continue;
                }
                
                // 마지막 시도에서도 실패하면 예외 발생
                throw new RuntimeException("Failed to wait for distribution deployment: " + e.getMessage(), e);
            }
        }
        
        // 타임아웃 발생
        log.error("[AwsCloudFrontManagementAdapter] Timeout waiting for distribution deployment: distributionId={}, maxWaitMinutes={}", 
            distributionId, maxWaitMinutes);
        throw new RuntimeException(
            String.format("Distribution deployment timeout: distributionId=%s, maxWaitMinutes=%d", 
                distributionId, maxWaitMinutes)
        );
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}

