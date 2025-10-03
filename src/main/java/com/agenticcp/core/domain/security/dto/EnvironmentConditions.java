package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 환경 조건 데이터 전송 객체
 * 
 * <p>정책의 환경 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentConditions {
    
    /**
     * 허용 환경 목록
     */
    private List<String> allowedEnvironments;
    
    /**
     * 금지 환경 목록
     */
    private List<String> deniedEnvironments;
    
    /**
     * 허용 테넌트 목록
     */
    private List<String> allowedTenants;
    
    /**
     * 금지 테넌트 목록
     */
    private List<String> deniedTenants;
    
    /**
     * 허용 리전 목록
     */
    private List<String> allowedRegions;
    
    /**
     * 금지 리전 목록
     */
    private List<String> deniedRegions;
    
    /**
     * 환경이 허용되는지 확인
     * 
     * @param environment 확인할 환경
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isEnvironmentAllowed(String environment) {
        if (environment == null || environment.isEmpty()) {
            return false;
        }
        
        // 금지 환경 확인
        if (deniedEnvironments != null && deniedEnvironments.contains(environment)) {
            return false;
        }
        
        // 허용 환경 확인
        if (allowedEnvironments != null && !allowedEnvironments.isEmpty()) {
            return allowedEnvironments.contains(environment);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 테넌트가 허용되는지 확인
     * 
     * @param tenant 확인할 테넌트
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isTenantAllowed(String tenant) {
        if (tenant == null || tenant.isEmpty()) {
            return false;
        }
        
        // 금지 테넌트 확인
        if (deniedTenants != null && deniedTenants.contains(tenant)) {
            return false;
        }
        
        // 허용 테넌트 확인
        if (allowedTenants != null && !allowedTenants.isEmpty()) {
            return allowedTenants.contains(tenant);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 리전이 허용되는지 확인
     * 
     * @param region 확인할 리전
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isRegionAllowed(String region) {
        if (region == null || region.isEmpty()) {
            return false;
        }
        
        // 금지 리전 확인
        if (deniedRegions != null && deniedRegions.contains(region)) {
            return false;
        }
        
        // 허용 리전 확인
        if (allowedRegions != null && !allowedRegions.isEmpty()) {
            return allowedRegions.contains(region);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
}
