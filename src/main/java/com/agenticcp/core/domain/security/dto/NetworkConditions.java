package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 네트워크 조건 데이터 전송 객체
 * 
 * <p>정책의 네트워크 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkConditions {
    
    /**
     * 허용 프로토콜 목록
     */
    private List<String> allowedProtocols;
    
    /**
     * 금지 프로토콜 목록
     */
    private List<String> deniedProtocols;
    
    /**
     * 허용 포트 목록
     */
    private List<Integer> allowedPorts;
    
    /**
     * 금지 포트 목록
     */
    private List<Integer> deniedPorts;
    
    /**
     * 허용 User-Agent 목록
     */
    private List<String> allowedUserAgents;
    
    /**
     * 금지 User-Agent 목록
     */
    private List<String> deniedUserAgents;
    
    /**
     * 프로토콜이 허용되는지 확인
     * 
     * @param protocol 확인할 프로토콜
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isProtocolAllowed(String protocol) {
        if (protocol == null || protocol.isEmpty()) {
            return false;
        }
        
        // 금지 프로토콜 확인
        if (deniedProtocols != null && deniedProtocols.contains(protocol.toUpperCase())) {
            return false;
        }
        
        // 허용 프로토콜 확인
        if (allowedProtocols != null && !allowedProtocols.isEmpty()) {
            return allowedProtocols.contains(protocol.toUpperCase());
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
    
    /**
     * 포트가 허용되는지 확인
     * 
     * @param port 확인할 포트
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isPortAllowed(Integer port) {
        if (port == null || port < 0 || port > 65535) {
            return false;
        }
        
        // 금지 포트 확인
        if (deniedPorts != null && deniedPorts.contains(port)) {
            return false;
        }
        
        // 허용 포트 확인
        if (allowedPorts != null && !allowedPorts.isEmpty()) {
            return allowedPorts.contains(port);
        }
        
        return true; // 허용 목록이 없으면 기본적으로 허용
    }
}
