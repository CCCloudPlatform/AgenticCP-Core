package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * 가상머신(Virtual Machine) 조회 조건을 정의하는 모델
 * AWS EC2 등 VM 조회 API의 요청 조건을 도메인 중심으로 추상화합니다.
 */
@Value
@Builder
public class VmQuery {
    
    /**
     * 특정 인스턴스 ID로 조회 (단일 인스턴스 조회 시 사용)
     */
    String instanceId;
    
    /**
     * 인스턴스 이름으로 필터링 (Name 태그 값)
     */
    String instanceName;
    
    /**
     * 인스턴스 상태로 필터링
     * AWS: pending, running, shutting-down, terminated, stopping, stopped
     */
    String state;
    
    /**
     * 인스턴스 타입으로 필터링 (예: t2.micro, t3.small)
     */
    String instanceType;
    
    /**
     * 가용 영역으로 필터링 (예: us-east-1a, ap-northeast-2a)
     */
    String availabilityZone;
    
    /**
     * 태그로 필터링 (키-값 쌍)
     * 예: {"Environment": "production", "Project": "webapp"}
     */
    Map<String, String> tags;
    
    /**
     * 페이징 - 페이지 번호 (0부터 시작)
     */
    @Builder.Default
    int page = 0;
    
    /**
     * 페이징 - 페이지 크기
     */
    @Builder.Default
    int size = 20;
    
    /**
     * 모든 인스턴스 조회를 위한 기본 쿼리 생성
     */
    public static VmQuery all() {
        return VmQuery.builder().build();
    }
    
    /**
     * 특정 인스턴스 ID로 조회하는 쿼리 생성
     */
    public static VmQuery byInstanceId(String instanceId) {
        return VmQuery.builder()
            .instanceId(instanceId)
            .build();
    }
    
    /**
     * 특정 상태의 인스턴스만 조회하는 쿼리 생성
     */
    public static VmQuery byState(String state) {
        return VmQuery.builder()
            .state(state)
            .build();
    }
    
    /**
     * 특정 인스턴스 타입만 조회하는 쿼리 생성
     */
    public static VmQuery byInstanceType(String instanceType) {
        return VmQuery.builder()
            .instanceType(instanceType)
            .build();
    }
}
