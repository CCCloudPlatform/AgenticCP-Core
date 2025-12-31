package com.agenticcp.core.domain.cloud.adapter.outbound.aws.dns;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsDnsConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.dns.DnsManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

/**
 * AWS Route53 DNS 관리 어댑터
 * 
 * AWS SDK를 사용하여 Route53 호스팅 존 관리 기능을 제공하는 어댑터
 * Usecase에서 전달받은 세션 자격증명만을 사용하여 AWS SDK를 호출합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsDnsManagementAdapter implements DnsManagementPort, ProviderScoped {

    private final AwsDnsConfig dnsConfig;
    private final AwsDnsMapper mapper;

    @Override
    public CloudResource createHostedZone(DnsCreateCommand command) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsManagementAdapter] Creating hosted zone: zoneName={}, zoneType={}", 
                    command.zoneName(), command.zoneType());

            route53Client = dnsConfig.createRoute53Client(command.session(), command.region());

            CreateHostedZoneRequest request = mapper.toCreateRequest(command);
            CreateHostedZoneResponse response = route53Client.createHostedZone(request);

            log.info("[AwsDnsManagementAdapter] Hosted zone created successfully: zoneId={}, zoneName={}", 
                    response.hostedZone().id(), command.zoneName());

            return mapper.toCloudResource(response, command);
        } catch (Throwable t) {
            log.error("[AwsDnsManagementAdapter] Failed to create hosted zone: zoneName={}", 
                    command.zoneName(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (route53Client != null) {
                route53Client.close();
            }
        }
    }

    @Override
    public CloudResource updateHostedZone(DnsUpdateCommand command) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsManagementAdapter] Updating hosted zone: zoneId={}", 
                    command.providerResourceId());

            route53Client = dnsConfig.createRoute53Client(command.session(), command.region());

            // Route53은 호스팅 존의 comment만 업데이트 가능
            // 태그는 별도 API로 관리해야 함
            if (command.comment() != null) {
                UpdateHostedZoneCommentRequest updateRequest = UpdateHostedZoneCommentRequest.builder()
                        .id(extractFullZoneId(command.providerResourceId()))
                        .comment(command.comment())
                        .build();
                
                route53Client.updateHostedZoneComment(updateRequest);
                log.debug("[AwsDnsManagementAdapter] Hosted zone comment updated: zoneId={}", 
                        command.providerResourceId());
            }

            // 업데이트된 호스팅 존 정보 조회
            GetHostedZoneRequest getRequest = GetHostedZoneRequest.builder()
                    .id(extractFullZoneId(command.providerResourceId()))
                    .build();
            
            GetHostedZoneResponse getResponse = route53Client.getHostedZone(getRequest);
            
            log.info("[AwsDnsManagementAdapter] Hosted zone updated successfully: zoneId={}", 
                    command.providerResourceId());

            return mapper.toCloudResource(getResponse.hostedZone(), command);
        } catch (Throwable t) {
            log.error("[AwsDnsManagementAdapter] Failed to update hosted zone: zoneId={}", 
                    command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (route53Client != null) {
                route53Client.close();
            }
        }
    }

    @Override
    public void deleteHostedZone(DnsDeleteCommand command) {
        Route53Client route53Client = null;
        try {
            log.debug("[AwsDnsManagementAdapter] Deleting hosted zone: zoneId={}, forceDelete={}", 
                    command.providerResourceId(), command.forceDelete());

            route53Client = dnsConfig.createRoute53Client(command.session(), command.region());

            DeleteHostedZoneRequest deleteRequest = DeleteHostedZoneRequest.builder()
                    .id(extractFullZoneId(command.providerResourceId()))
                    .build();

            route53Client.deleteHostedZone(deleteRequest);

            log.info("[AwsDnsManagementAdapter] Hosted zone deleted successfully: zoneId={}", 
                    command.providerResourceId());
        } catch (Throwable t) {
            log.error("[AwsDnsManagementAdapter] Failed to delete hosted zone: zoneId={}", 
                    command.providerResourceId(), t);
            
            // Route53은 레코드가 있으면 삭제할 수 없음
            // forceDelete 옵션은 Route53에서 지원하지 않으므로 에러 메시지만 확인
            if (t.getMessage() != null && t.getMessage().contains("HostedZoneNotEmpty")) {
                log.warn("[AwsDnsManagementAdapter] Hosted zone contains records and cannot be deleted: zoneId={}", 
                        command.providerResourceId());
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
     * Zone ID를 Route53 API 형식으로 변환
     * Route53 API는 /hostedzone/Z1234567890 형식을 요구하지만,
     * 우리는 Z1234567890만 저장하므로 필요시 /hostedzone/ 접두사 추가
     */
    private String extractFullZoneId(String zoneId) {
        if (zoneId == null) {
            return null;
        }
        // 이미 /hostedzone/ 접두사가 있으면 그대로 반환
        if (zoneId.startsWith("/hostedzone/")) {
            return zoneId;
        }
        // 없으면 추가
        return "/hostedzone/" + zoneId;
    }
}
