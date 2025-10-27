package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * EC2 인스턴스 조회 조건을 정의하는 모델
 * AWS EC2 API의 DescribeInstances 요청 조건을 도메인 중심으로 추상화
 */
@Value
@Builder
public class Ec2Query {
    
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
    public static Ec2Query all() {
        return Ec2Query.builder().build();
    }
    
    /**
     * 특정 인스턴스 ID로 조회하는 쿼리 생성
     */
    public static Ec2Query byInstanceId(String instanceId) {
        return Ec2Query.builder()
            .instanceId(instanceId)
            .build();
    }
    
    /**
     * 특정 상태의 인스턴스만 조회하는 쿼리 생성
     */
    public static Ec2Query byState(String state) {
        return Ec2Query.builder()
            .state(state)
            .build();
    }
    
    /**
     * 특정 인스턴스 타입만 조회하는 쿼리 생성
     */
    public static Ec2Query byInstanceType(String instanceType) {
        return Ec2Query.builder()
            .instanceType(instanceType)
            .build();
    }
}
