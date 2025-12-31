package com.agenticcp.core.domain.cloud.port.outbound.dns;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.dns.DnsUpdateCommand;

/**
 * DNS 호스팅 존 관리 포트
 *
 * 각 CSP별 Adapter(AWS Route53, Azure DNS, GCP Cloud DNS 등)가 이 인터페이스를 구현합니다.
 */
public interface DnsManagementPort {

    /**
     * DNS 호스팅 존 생성
     */
    CloudResource createHostedZone(DnsCreateCommand command);

    /**
     * DNS 호스팅 존 수정
     */
    CloudResource updateHostedZone(DnsUpdateCommand command);

    /**
     * DNS 호스팅 존 삭제
     */
    void deleteHostedZone(DnsDeleteCommand command);
}

