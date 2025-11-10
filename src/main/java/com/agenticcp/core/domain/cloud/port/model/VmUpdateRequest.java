package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 가상머신(Virtual Machine) 수정 요청을 정의하는 모델
 * AWS EC2 등 VM 설정 변경 API를 도메인 중심으로 추상화합니다.
 */
@Value
@Builder
public class VmUpdateRequest {
    
    /**
     * 수정할 인스턴스 ID
     */
    String instanceId;
    
    /**
     * 새로운 인스턴스 타입 (인스턴스 타입 변경 시)
     * 예: t2.micro -> t3.small
     */
    String instanceType;
    
    /**
     * 새로운 사용자 데이터 (스크립트 또는 클라우드-초기화 데이터)
     * Base64 인코딩된 문자열
     */
    String userData;
    
    /**
     * 추가할 태그 (기존 태그와 병합)
     * 예: {"Environment": "staging", "Updated": "2024-01-01"}
     */
    Map<String, String> tagsToAdd;
    
    /**
     * 제거할 태그 키 목록
     * 예: ["OldTag", "DeprecatedTag"]
     */
    Map<String, String> tagsToRemove;
    
    /**
     * 인스턴스 타입만 변경하는 요청 생성
     */
    public static VmUpdateRequest changeInstanceType(String instanceId, String newInstanceType) {
        return VmUpdateRequest.builder()
            .instanceId(instanceId)
            .instanceType(newInstanceType)
            .build();
    }
    
    /**
     * 사용자 데이터만 변경하는 요청 생성
     */
    public static VmUpdateRequest changeUserData(String instanceId, String newUserData) {
        return VmUpdateRequest.builder()
            .instanceId(instanceId)
            .userData(newUserData)
            .build();
    }
    
    /**
     * 태그만 추가하는 요청 생성
     */
    public static VmUpdateRequest addTags(String instanceId, Map<String, String> tags) {
        return VmUpdateRequest.builder()
            .instanceId(instanceId)
            .tagsToAdd(tags)
            .build();
    }
    
    /**
     * 태그만 제거하는 요청 생성
     */
    public static VmUpdateRequest removeTags(String instanceId, Map<String, String> tags) {
        return VmUpdateRequest.builder()
            .instanceId(instanceId)
            .tagsToRemove(tags)
            .build();
    }
    
    /**
     * 환경을 변경하는 요청 생성 (개발 -> 스테이징 -> 프로덕션)
     */
    public static VmUpdateRequest changeEnvironment(String instanceId, String environment) {
        return VmUpdateRequest.builder()
            .instanceId(instanceId)
            .tagsToAdd(Map.of("Environment", environment))
            .build();
    }
}
