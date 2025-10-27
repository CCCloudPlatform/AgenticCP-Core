package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * EC2 인스턴스 생성 요청을 정의하는 모델
 * AWS EC2 API의 RunInstances 요청을 도메인 중심으로 추상화
 */
@Value
@Builder
public class Ec2CreateRequest {
    
    /**
     * AMI ID (Amazon Machine Image)
     * 예: ami-0abcdef1234567890
     */
    String imageId;
    
    /**
     * 인스턴스 타입 (예: t2.micro, t3.small, m5.large)
     */
    String instanceType;
    
    /**
     * 키 페어 이름 (SSH 접속용)
     * 예: my-key-pair
     */
    String keyName;
    
    /**
     * 보안 그룹 ID (여러 개일 경우 콤마로 구분)
     * 예: sg-12345678 또는 sg-12345678,sg-87654321
     */
    String securityGroupId;
    
    /**
     * 서브넷 ID (VPC 내 특정 서브넷)
     * 예: subnet-12345678
     */
    String subnetId;
    
    /**
     * 사용자 데이터 (스크립트 또는 클라우드-초기화 데이터)
     * Base64 인코딩된 문자열
     */
    String userData;
    
    /**
     * 인스턴스에 적용할 태그
     * 예: {"Name": "web-server", "Environment": "production"}
     */
    Map<String, String> tags;
    
    /**
     * 최소 생성 인스턴스 수
     */
    @Builder.Default
    int minCount = 1;
    
    /**
     * 최대 생성 인스턴스 수
     */
    @Builder.Default
    int maxCount = 1;
    
    /**
     * 기본 인스턴스 생성 요청 생성
     */
    public static Ec2CreateRequest basic(String imageId, String instanceType) {
        return Ec2CreateRequest.builder()
            .imageId(imageId)
            .instanceType(instanceType)
            .minCount(1)
            .maxCount(1)
            .build();
    }
    
    /**
     * 웹 서버용 인스턴스 생성 요청 생성
     */
    public static Ec2CreateRequest webServer(String imageId, String instanceType, String keyName) {
        return Ec2CreateRequest.builder()
            .imageId(imageId)
            .instanceType(instanceType)
            .keyName(keyName)
            .tags(Map.of(
                "Name", "web-server",
                "Environment", "production",
                "Service", "web"
            ))
            .build();
    }
    
    /**
     * 개발 환경용 인스턴스 생성 요청 생성
     */
    public static Ec2CreateRequest development(String imageId, String instanceType, String keyName) {
        return Ec2CreateRequest.builder()
            .imageId(imageId)
            .instanceType(instanceType)
            .keyName(keyName)
            .tags(Map.of(
                "Name", "dev-server",
                "Environment", "development",
                "Service", "development"
            ))
            .build();
    }
}
