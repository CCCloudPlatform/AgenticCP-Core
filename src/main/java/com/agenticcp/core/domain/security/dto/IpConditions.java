package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * IP 조건 데이터 전송 객체
 * 
 * <p>정책의 IP 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpConditions {
    
    /**
     * 허용 IP 주소 목록
     */
    private List<String> allowedIps;
    
    /**
     * 금지 IP 주소 목록
     */
    private List<String> deniedIps;
    
    /**
     * 허용 IP 대역 목록 (CIDR 형식)
     * 예: 192.168.1.0/24, 10.0.0.0/8
     */
    private List<String> allowedCidrs;
    
    /**
     * 금지 IP 대역 목록 (CIDR 형식)
     */
    private List<String> deniedCidrs;
    
    /**
     * 허용 국가 코드 목록 (ISO 3166-1 alpha-2)
     * 예: KR, US, JP
     */
    private List<String> allowedCountries;
    
    /**
     * 금지 국가 코드 목록
     */
    private List<String> deniedCountries;
    
    /**
     * 허용 지역 목록
     */
    private List<String> allowedRegions;
    
    /**
     * 금지 지역 목록
     */
    private List<String> deniedRegions;
    
    /**
     * IP 주소가 허용되는지 확인
     * 
     * @param ip 확인할 IP 주소
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isIpAllowed(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 금지 IP 확인
        if (deniedIps != null && deniedIps.contains(ip)) {
            return false;
        }
        
        // 금지 CIDR 확인
        if (deniedCidrs != null) {
            for (String cidr : deniedCidrs) {
                if (isIpInCidr(ip, cidr)) {
                    return false;
                }
            }
        }
        
        // 허용 IP 확인
        if (allowedIps != null && allowedIps.contains(ip)) {
            return true;
        }
        
        // 허용 CIDR 확인
        if (allowedCidrs != null) {
            for (String cidr : allowedCidrs) {
                if (isIpInCidr(ip, cidr)) {
                    return true;
                }
            }
        }
        
        // 허용 목록이 있으면 기본적으로 거부
        if ((allowedIps != null && !allowedIps.isEmpty()) || 
            (allowedCidrs != null && !allowedCidrs.isEmpty())) {
            return false;
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * IP 주소가 CIDR 대역에 포함되는지 확인
     * 
     * @param ip 확인할 IP 주소
     * @param cidr CIDR 대역 (예: 192.168.1.0/24)
     * @return 포함되면 true, 그렇지 않으면 false
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            // 간단한 CIDR 체크 (실제 구현에서는 더 정교한 로직 필요)
            if (cidr == null || !cidr.contains("/")) {
                return false;
            }
            
            String[] parts = cidr.split("/");
            String networkIp = parts[0];
            // int prefixLength = Integer.parseInt(parts[1]); // TODO: 실제 CIDR 매칭 로직 구현 시 사용
            
            // TODO: 실제 CIDR 매칭 로직 구현
            // 현재는 단순 문자열 비교로 대체
            return ip.startsWith(networkIp.substring(0, networkIp.lastIndexOf('.')));
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 국가가 허용되는지 확인
     * 
     * @param countryCode 확인할 국가 코드
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isCountryAllowed(String countryCode) {
        if (countryCode == null || countryCode.isEmpty()) {
            return false;
        }
        
        // 금지 국가 확인
        if (deniedCountries != null && deniedCountries.contains(countryCode.toUpperCase())) {
            return false;
        }
        
        // 허용 국가 확인
        if (allowedCountries != null && !allowedCountries.isEmpty()) {
            return allowedCountries.contains(countryCode.toUpperCase());
        }
        
        return true; // 허용 국가가 없으면 기본적으로 허용
    }
    
    /**
     * 지역이 허용되는지 확인
     * 
     * @param region 확인할 지역
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isRegionAllowed(String region) {
        if (region == null || region.isEmpty()) {
            return false;
        }
        
        // 금지 지역 확인
        if (deniedRegions != null && deniedRegions.contains(region)) {
            return false;
        }
        
        // 허용 지역 확인
        if (allowedRegions != null && !allowedRegions.isEmpty()) {
            return allowedRegions.contains(region);
        }
        
        return true; // 허용 지역이 없으면 기본적으로 허용
    }
}
