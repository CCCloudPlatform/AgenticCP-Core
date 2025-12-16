package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudResource.LifecycleState;
import com.agenticcp.core.domain.cloud.entity.CloudResource.ResourceType;
import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * CloudResource 등록 요청 DTO
 * 
 * 모든 클라우드 리소스(VM, Storage, VPC, RDS 등)를 통합적으로 등록하기 위한 요청 객체입니다.
 * 도메인별 상세 속성(instanceSize, cidrBlock 등)은 attributes에 담아 전달합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Getter
@Builder
public class ResourceRegistrationRequest {

    /**
     * 리소스 ID (CSP에서 부여한 고유 ID)
     * 예: AWS EC2 인스턴스 ID, VPC ID, S3 버킷 이름 등
     */
    private final String resourceId;

    /**
     * 리소스 이름 (사용자 지정 또는 태그에서 추출)
     */
    private final String resourceName;

    /**
     * 리소스 타입
     * @see ResourceType
     */
    private final ResourceType resourceType;

    /**
     * 리소스 태그 (CSP의 태그 정보)
     */
    @Builder.Default
    private final Map<String, String> tags = new HashMap<>();

    /**
     * 도메인별 상세 속성
     * 
     * <p>리소스 타입에 따른 속성 예시:</p>
     * <ul>
     *   <li>VM: instanceSize, cpuCores, memoryGb</li>
     *   <li>VPC: configuration (cidrBlock)</li>
     *   <li>Storage: storageGb</li>
     *   <li>RDS: engineVersion, storageType</li>
     * </ul>
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 초기 생명주기 상태 (선택적)
     * null인 경우 resourceType에 따라 기본값이 적용됩니다.
     */
    private final LifecycleState initialLifecycleState;

    // ==================== 편의 메서드 ====================

    /**
     * 속성 값을 String으로 조회합니다.
     * 
     * @param key 속성 키
     * @return 속성 값 또는 null
     */
    public String getAttributeAsString(String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 속성 값을 Integer로 조회합니다.
     * 
     * @param key 속성 키
     * @return 속성 값 또는 null
     */
    public Integer getAttributeAsInteger(String key) {
        Object value = attributes.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 속성 값을 Long으로 조회합니다.
     * 
     * @param key 속성 키
     * @return 속성 값 또는 null
     */
    public Long getAttributeAsLong(String key) {
        Object value = attributes.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ==================== 자주 사용되는 속성 키 상수 ====================

    /**
     * 자주 사용되는 속성 키 상수
     */
    public static final class AttributeKeys {
        /** VM 인스턴스 크기 (예: t3.micro, Standard_B1s) */
        public static final String INSTANCE_SIZE = "instanceSize";
        
        /** 리소스 설정 정보 (JSON 또는 CIDR 블록 등) */
        public static final String CONFIGURATION = "configuration";
        
        /** CPU 코어 수 */
        public static final String CPU_CORES = "cpuCores";
        
        /** 메모리 (GB) */
        public static final String MEMORY_GB = "memoryGb";
        
        /** 스토리지 (GB) */
        public static final String STORAGE_GB = "storageGb";
        
        /** 인스턴스 타입 */
        public static final String INSTANCE_TYPE = "instanceType";
        
        private AttributeKeys() {}
    }
}
