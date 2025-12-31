package com.agenticcp.core.domain.cloud.adapter.outbound.aws.dns;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudRegion;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudService;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsQuery;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.GetDnsCommand;
import com.agenticcp.core.domain.cloud.repository.CloudProviderRepository;
import com.agenticcp.core.domain.cloud.repository.CloudRegionRepository;
import com.agenticcp.core.domain.cloud.repository.CloudServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.CreateHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.HostedZoneConfig;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AWS Route53 DNS 매퍼
 * 
 * AWS SDK HostedZone 객체를 CloudResource로 변환하고,
 * DnsCreateCommand를 AWS SDK 요청으로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsDnsMapper {

    private final ObjectMapper objectMapper;
    private final CloudProviderRepository cloudProviderRepository;
    private final CloudServiceRepository cloudServiceRepository;
    private final CloudRegionRepository cloudRegionRepository;

    /**
     * AWS HostedZone → CloudResource 변환
     * 
     * @param hostedZone AWS HostedZone 객체
     * @param command DnsCreateCommand (생성 시) 또는 GetDnsCommand (조회 시)
     * @return CloudResource 엔티티
     */
    public CloudResource toCloudResource(HostedZone hostedZone, DnsCreateCommand command) {
        return buildCloudResource(hostedZone, command.providerType(), command.serviceKey(), 
                command.region(), command.tags(), null);
    }

    /**
     * AWS HostedZone → CloudResource 변환 (GetDnsCommand 사용)
     */
    public CloudResource toCloudResource(HostedZone hostedZone, GetDnsCommand command) {
        return buildCloudResource(hostedZone, command.providerType(), command.serviceKey(), 
                command.region(), null, null);
    }

    /**
     * AWS HostedZone → CloudResource 변환 (DnsQuery 사용)
     */
    public CloudResource toCloudResource(HostedZone hostedZone, DnsQuery query) {
        return buildCloudResource(hostedZone, query.providerType(), "ROUTE53", 
                null, query.tagsEquals(), null);
    }

    /**
     * AWS HostedZone → CloudResource 변환 (DnsUpdateCommand 사용)
     */
    public CloudResource toCloudResource(HostedZone hostedZone, DnsUpdateCommand command) {
        return buildCloudResource(hostedZone, command.providerType(), "ROUTE53", 
                command.region(), command.tags(), null);
    }

    /**
     * CreateHostedZoneResponse → CloudResource 변환
     */
    public CloudResource toCloudResource(CreateHostedZoneResponse response, DnsCreateCommand command) {
        HostedZone hostedZone = response.hostedZone();
        List<String> nameServers = response.delegationSet() != null && response.delegationSet().nameServers() != null
                ? response.delegationSet().nameServers()
                : List.of();
        
        return buildCloudResource(hostedZone, command.providerType(), command.serviceKey(), 
                command.region(), command.tags(), nameServers);
    }

    /**
     * DnsCreateCommand → CreateHostedZoneRequest 변환
     * CSP 중립적 Command를 AWS SDK 요청으로 변환
     */
    public CreateHostedZoneRequest toCreateRequest(DnsCreateCommand command) {
        String zoneName = command.zoneName();
        // Route53은 zone name 끝에 trailing dot(.)을 요구하지 않지만, 일관성을 위해 추가
        if (!zoneName.endsWith(".")) {
            zoneName = zoneName + ".";
        }

        CreateHostedZoneRequest.Builder builder = CreateHostedZoneRequest.builder()
                .name(zoneName)
                .callerReference(UUID.randomUUID().toString());  // Route53 요구사항

        // Private Zone인 경우 VPC 설정
        if ("PRIVATE".equals(command.zoneType())) {
            HostedZoneConfig.Builder configBuilder = HostedZoneConfig.builder()
                    .privateZone(true);
            
            if (command.comment() != null) {
                configBuilder.comment(command.comment());
            }
            
            builder.hostedZoneConfig(configBuilder.build());

            // VPC ID가 제공된 경우 VPC 설정
            if (command.vpcId() != null && !command.vpcId().isEmpty()) {
                // Route53의 VPC는 Consumer를 통해 설정
                String vpcRegion = command.region() != null && !command.region().isEmpty() 
                        ? command.region() 
                        : "us-east-1"; // 기본값
                builder.vpc(vpcBuilder -> vpcBuilder
                        .vpcId(command.vpcId())
                        .vpcRegion(vpcRegion));
            }
        } else {
            // Public Zone
            HostedZoneConfig.Builder configBuilder = HostedZoneConfig.builder()
                    .privateZone(false);
            
            if (command.comment() != null) {
                configBuilder.comment(command.comment());
            }
            
            builder.hostedZoneConfig(configBuilder.build());
        }

        return builder.build();
    }

    /**
     * CloudResource 빌드 (공통 로직)
     */
    private CloudResource buildCloudResource(
            HostedZone hostedZone,
            CloudProvider.ProviderType providerType,
            String serviceKey,
            String region,
            Map<String, String> tags,
            List<String> nameServers) {
        
        try {
            log.debug("[AwsDnsMapper] Converting AWS HostedZone to CloudResource: {}", hostedZone.id());

            // Zone name에서 trailing dot 제거 (CSP 중립적)
            String zoneName = extractZoneName(hostedZone.name());
            String resourceId = extractZoneId(hostedZone.id());

            // 엔티티 조회
            CloudProvider provider = cloudProviderRepository.findFirstByProviderType(providerType)
                    .orElseThrow(() -> new IllegalStateException("CloudProvider not found for type: " + providerType));

            CloudService service = cloudServiceRepository.findByProviderTypeAndServiceKey(providerType, serviceKey)
                    .orElseThrow(() -> new IllegalStateException("CloudService not found for providerType: " + providerType + ", serviceKey: " + serviceKey));

            // Route53은 글로벌 서비스이므로 region은 optional
            CloudRegion cloudRegion = null;
            if (region != null && !region.isEmpty()) {
                cloudRegion = cloudRegionRepository.findByProviderTypeAndRegionKey(providerType, region)
                        .orElse(null);
                if (cloudRegion == null) {
                    log.warn("[AwsDnsMapper] CloudRegion not found for providerType: {}, regionKey: {}", providerType, region);
                }
            }

            // Configuration JSON 구성
            String configurationJson = buildConfigurationJson(hostedZone, nameServers);

            // Metadata JSON 구성
            String metadataJson = buildMetadataJson(hostedZone, nameServers);

            // LifecycleState 매핑
            CloudResource.LifecycleState lifecycleState = mapLifecycleState(hostedZone);

            // CreatedInCloud 시간 변환
            // HostedZone에는 생성 시간 정보가 없으므로 현재 시간 사용
            LocalDateTime createdInCloud = LocalDateTime.now();

            return CloudResource.builder()
                    .resourceId(resourceId)
                    .resourceName(zoneName)
                    .displayName(zoneName)
                    .provider(provider)
                    .service(service)
                    .region(cloudRegion)
                    .resourceType(CloudResource.ResourceType.DNS_ZONE)
                    .lifecycleState(lifecycleState)
                    .tags(tags != null ? tags : new HashMap<>())
                    .configuration(configurationJson)
                    .metadata(metadataJson)
                    .createdInCloud(createdInCloud)
                    .lastSync(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("[AwsDnsMapper] Failed to convert AWS HostedZone to CloudResource: {}", hostedZone.id(), e);
            throw new BusinessException(CloudErrorCode.MAPPING_FAILED,
                    "DNS 호스팅 존을 CloudResource로 변환하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Zone name에서 trailing dot 제거
     */
    private String extractZoneName(String zoneName) {
        if (zoneName == null) {
            return null;
        }
        // Route53은 zone name 끝에 trailing dot(.)을 포함
        return zoneName.endsWith(".") ? zoneName.substring(0, zoneName.length() - 1) : zoneName;
    }

    /**
     * Zone ID에서 호스팅 존 ID만 추출
     * Route53의 zone ID 형식: /hostedzone/Z1234567890
     */
    private String extractZoneId(String zoneId) {
        if (zoneId == null) {
            return null;
        }
        // /hostedzone/Z1234567890 형식에서 Z1234567890만 추출
        if (zoneId.startsWith("/hostedzone/")) {
            return zoneId.substring("/hostedzone/".length());
        }
        return zoneId;
    }

    /**
     * Configuration JSON 구성
     */
    private String buildConfigurationJson(HostedZone hostedZone, List<String> nameServers) {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("zoneName", extractZoneName(hostedZone.name()));
            config.put("zoneId", extractZoneId(hostedZone.id()));
            config.put("isPrivateZone", hostedZone.config() != null && Boolean.TRUE.equals(hostedZone.config().privateZone()));
            config.put("comment", hostedZone.config() != null ? hostedZone.config().comment() : null);
            
            if (nameServers != null && !nameServers.isEmpty()) {
                config.put("nameServers", nameServers);
            }

            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.warn("[AwsDnsMapper] Failed to serialize configuration: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Metadata JSON 구성
     */
    private String buildMetadataJson(HostedZone hostedZone, List<String> nameServers) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("zoneName", extractZoneName(hostedZone.name()));
            metadata.put("zoneId", extractZoneId(hostedZone.id()));
            metadata.put("isPrivateZone", hostedZone.config() != null && Boolean.TRUE.equals(hostedZone.config().privateZone()));
            metadata.put("comment", hostedZone.config() != null ? hostedZone.config().comment() : null);
            metadata.put("recordSetCount", hostedZone.resourceRecordSetCount() != null ? hostedZone.resourceRecordSetCount() : 0);
            
            if (nameServers != null && !nameServers.isEmpty()) {
                metadata.put("nameServers", nameServers);
            }

            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("[AwsDnsMapper] Failed to serialize metadata: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * LifecycleState 매핑
     */
    private CloudResource.LifecycleState mapLifecycleState(HostedZone hostedZone) {
        // Route53 HostedZone은 항상 활성 상태이므로 RUNNING으로 매핑
        // 삭제 중인 경우는 별도로 처리 필요 (현재는 RUNNING으로 처리)
        return CloudResource.LifecycleState.RUNNING;
    }
}
