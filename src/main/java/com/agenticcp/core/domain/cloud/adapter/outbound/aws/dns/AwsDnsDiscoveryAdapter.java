package com.agenticcp.core.domain.cloud.adapter.outbound.aws.dns;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDnsConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsQuery;
import com.agenticcp.core.domain.cloud.port.outbound.dns.DnsDiscoveryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.GetHostedZoneRequest;
import software.amazon.awssdk.services.route53.model.GetHostedZoneResponse;
import software.amazon.awssdk.services.route53.model.HostedZone;
import software.amazon.awssdk.services.route53.model.ListHostedZonesRequest;
import software.amazon.awssdk.services.route53.model.ListHostedZonesResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS Route53 DNS 발견 어댑터
 * 
 * AWS SDK를 사용하여 Route53 호스팅 존 조회 기능을 제공하는 어댑터
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsDnsDiscoveryAdapter implements DnsDiscoveryPort, ProviderScoped {

    private final AwsDnsConfig dnsConfig;
    private final AwsDnsMapper mapper;

    @Override
    public Page<CloudResource> listHostedZones(DnsQuery query, CloudSessionCredential session) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsDiscoveryAdapter] Listing hosted zones: providerType={}, accountScope={}", 
                    query.providerType(), query.accountScope());

            route53Client = dnsConfig.createRoute53Client(session, null);

            // AWS Route53 ListHostedZones 호출
            ListHostedZonesRequest.Builder requestBuilder = ListHostedZonesRequest.builder();

            // Route53은 제한적인 필터링만 지원
            // zoneName으로 시작하는 존만 조회 (prefix 기반)
            if (query.zoneName() != null && !query.zoneName().isEmpty()) {
                // Route53은 dnsname 필터를 지원하지 않으므로,
                // 모든 존을 조회한 후 메모리에서 필터링
                // 하지만 일단 모든 존을 조회
            }

            ListHostedZonesResponse response = route53Client.listHostedZones(requestBuilder.build());

            // HostedZone 목록을 CloudResource로 변환
            List<CloudResource> resources = response.hostedZones().stream()
                    .map(zone -> {
                        // 메모리에서 필터링
                        if (query.zoneName() != null && !query.zoneName().isEmpty()) {
                            String zoneName = extractZoneName(zone.name());
                            if (!zoneName.equals(query.zoneName()) && !zoneName.startsWith(query.zoneName())) {
                                return null;
                            }
                        }

                        // zoneType 필터링 (PUBLIC/PRIVATE)
                        if (query.zoneType() != null && !query.zoneType().isEmpty()) {
                            boolean isPrivate = zone.config() != null && Boolean.TRUE.equals(zone.config().privateZone());
                            String zoneType = isPrivate ? "PRIVATE" : "PUBLIC";
                            if (!zoneType.equals(query.zoneType())) {
                                return null;
                            }
                        }

                        // VPC ID 필터링 (Private Zone인 경우)
                        if (query.vpcId() != null && !query.vpcId().isEmpty()) {
                            // Route53 ListHostedZones는 VPC 정보를 포함하지 않으므로
                            // GetHostedZone으로 상세 정보를 조회해야 함
                            // 성능상의 이유로 여기서는 필터링하지 않고 모든 존을 반환
                            // 필요시 GetHostedZone으로 상세 조회 후 필터링
                        }

                        return mapper.toCloudResource(zone, query);
                    })
                    .filter(resource -> resource != null)
                    .collect(Collectors.toList());

            // 페이징 처리
            int page = query.page();
            int size = query.size();
            int total = resources.size();
            int start = page * size;
            int end = Math.min(start + size, total);

            List<CloudResource> pagedResources = start < total 
                    ? resources.subList(start, end)
                    : List.of();

            log.info("[AwsDnsDiscoveryAdapter] Listed hosted zones: total={}, page={}, size={}, returned={}", 
                    total, page, size, pagedResources.size());

            return new PageImpl<>(pagedResources, PageRequest.of(page, size), total);
        } catch (Throwable t) {
            log.error("[AwsDnsDiscoveryAdapter] Failed to list hosted zones", t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (route53Client != null) {
                route53Client.close();
            }
        }
    }

    @Override
    public Optional<CloudResource> getHostedZone(String zoneId, CloudSessionCredential session) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsDiscoveryAdapter] Getting hosted zone: zoneId={}", zoneId);

            route53Client = dnsConfig.createRoute53Client(session, null);

            GetHostedZoneRequest request = GetHostedZoneRequest.builder()
                    .id(extractFullZoneId(zoneId))
                    .build();

            GetHostedZoneResponse response = route53Client.getHostedZone(request);

            if (response.hostedZone() == null) {
                log.warn("[AwsDnsDiscoveryAdapter] Hosted zone not found: zoneId={}", zoneId);
                return Optional.empty();
            }

            // GetDnsCommand를 생성하여 매퍼에 전달
            // 여기서는 간단히 null을 전달하고 매퍼에서 처리
            CloudResource resource = mapper.toCloudResource(response.hostedZone(), 
                    com.agenticcp.core.domain.cloud.port.model.dns.GetDnsCommand.builder()
                            .providerType(ProviderType.AWS)
                            .accountScope(null)
                            .region(null)
                            .providerResourceId(zoneId)
                            .serviceKey("ROUTE53")
                            .resourceType("DNS_ZONE")
                            .session(session)
                            .build());

            log.info("[AwsDnsDiscoveryAdapter] Got hosted zone: zoneId={}, zoneName={}", 
                    zoneId, extractZoneName(response.hostedZone().name()));

            return Optional.of(resource);
        } catch (Throwable t) {
            log.error("[AwsDnsDiscoveryAdapter] Failed to get hosted zone: zoneId={}", zoneId, t);
            
            // Route53에서 NotFoundException이 발생하면 Optional.empty() 반환
            if (t.getMessage() != null && 
                    (t.getMessage().contains("NoSuchHostedZone") || 
                     t.getMessage().contains("404"))) {
                return Optional.empty();
            }
            
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (route53Client != null) {
                route53Client.close();
            }
        }
    }

    @Override
    public String getZoneStatus(String zoneId, CloudSessionCredential session) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsDiscoveryAdapter] Getting zone status: zoneId={}", zoneId);

            route53Client = dnsConfig.createRoute53Client(session, null);

            GetHostedZoneRequest request = GetHostedZoneRequest.builder()
                    .id(extractFullZoneId(zoneId))
                    .build();

            GetHostedZoneResponse response = route53Client.getHostedZone(request);

            if (response.hostedZone() == null) {
                return "NOT_FOUND";
            }

            // Route53 HostedZone은 항상 활성 상태
            return "ACTIVE";
        } catch (Throwable t) {
            log.error("[AwsDnsDiscoveryAdapter] Failed to get zone status: zoneId={}", zoneId, t);
            
            if (t.getMessage() != null && 
                    (t.getMessage().contains("NoSuchHostedZone") || 
                     t.getMessage().contains("404"))) {
                return "NOT_FOUND";
            }
            
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (route53Client != null) {
                route53Client.close();
            }
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    /**
     * Zone name에서 trailing dot 제거
     */
    private String extractZoneName(String zoneName) {
        if (zoneName == null) {
            return null;
        }
        return zoneName.endsWith(".") ? zoneName.substring(0, zoneName.length() - 1) : zoneName;
    }

    /**
     * Zone ID를 Route53 API 형식으로 변환
     */
    private String extractFullZoneId(String zoneId) {
        if (zoneId == null) {
            return null;
        }
        if (zoneId.startsWith("/hostedzone/")) {
            return zoneId;
        }
        return "/hostedzone/" + zoneId;
    }
}
