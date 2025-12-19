package com.agenticcp.core.domain.cloud.port.outbound.dns;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsQuery;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * DNS 호스팅 존 조회 포트
 */
public interface DnsDiscoveryPort {

    /**
     * DNS 호스팅 존 목록 조회
     */
    Page<CloudResource> listHostedZones(DnsQuery query, CloudSessionCredential session);

    /**
     * 특정 DNS 호스팅 존 조회
     */
    Optional<CloudResource> getHostedZone(String zoneId, CloudSessionCredential session);

    /**
     * DNS 호스팅 존 상태 조회
     */
    String getZoneStatus(String zoneId, CloudSessionCredential session);
}

