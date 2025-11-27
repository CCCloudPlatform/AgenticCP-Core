package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * 가상머신(Virtual Machine) 생성 요청을 정의하는 CSP 중립적 모델
 * 
 * 모든 클라우드 프로바이더에서 공통으로 사용할 수 있는 추상화된 필드를 정의합니다.
 * 각 CSP Adapter에서 해당 필드를 CSP 특화 요청으로 변환합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Value
@Builder
@Jacksonized
public class VmCreateRequest {
    
    /**
     * VM 이미지 식별자 (CSP별로 해석)
     * - AWS: AMI ID (예: ami-0abcdef1234567890)
     * - GCP: Image family/project (예: projects/debian-cloud/global/images/debian-11)
     * - Azure: Image reference (예: Canonical:UbuntuServer:18.04-LTS:latest)
     */
    String image;
    
    /**
     * 인스턴스 크기/타입 (CSP별로 매핑)
     * - AWS: t2.micro, t3.small, m5.large 등
     * - GCP: n1-standard-1, e2-medium 등
     * - Azure: Standard_DS1_v2, Standard_B1s 등
     */
    String instanceSize;
    
    /**
     * SSH 키 이름 또는 공개 키
     * - AWS: Key Pair 이름
     * - GCP: SSH 공개 키
     * - Azure: SSH 공개 키
     */
    String sshKey;
    
    /**
     * 네트워크/보안 그룹 식별자 (CSP별로 해석)
     * - AWS: Security Group ID (예: sg-12345678)
     * - GCP: Firewall rule 이름
     * - Azure: Network Security Group ID
     */
    String networkSecurityId;
    
    /**
     * 서브넷 식별자 (CSP별로 해석)
     * - AWS: Subnet ID (예: subnet-12345678)
     * - GCP: Subnetwork (예: projects/xxx/regions/xxx/subnetworks/xxx)
     * - Azure: Subnet resource ID
     */
    String subnetId;
    
    /**
     * 리전/가용영역 (CSP별로 해석)
     * - AWS: us-east-1a
     * - GCP: us-central1-a
     * - Azure: eastus
     */
    String zone;
    
    /**
     * 사용자 데이터 (스크립트 또는 클라우드-초기화 데이터)
     * Base64 인코딩된 문자열 또는 plain text
     */
    String userData;
    
    /**
     * 인스턴스에 적용할 태그/라벨
     * 예: {"Name": "web-server", "Environment": "production"}
     */
    Map<String, String> tags;
    
    /**
     * 최소 생성 인스턴스 수
     */
    @Min(1)
    @Builder.Default
    int minCount = 1;
    
    /**
     * 최대 생성 인스턴스 수
     */
    @Min(1)
    @Builder.Default
    int maxCount = 1;
    
    /**
     * 기본 인스턴스 생성 요청 생성
     */
    public static VmCreateRequest basic(String image, String instanceSize) {
        return VmCreateRequest.builder()
            .image(image)
            .instanceSize(instanceSize)
            .minCount(1)
            .maxCount(1)
            .build();
    }
    
    /**
     * 웹 서버용 인스턴스 생성 요청 생성
     */
    public static VmCreateRequest webServer(String image, String instanceSize, String sshKey) {
        return VmCreateRequest.builder()
            .image(image)
            .instanceSize(instanceSize)
            .sshKey(sshKey)
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
    public static VmCreateRequest development(String image, String instanceSize, String sshKey) {
        return VmCreateRequest.builder()
            .image(image)
            .instanceSize(instanceSize)
            .sshKey(sshKey)
            .tags(Map.of(
                "Name", "dev-server",
                "Environment", "development",
                "Service", "development"
            ))
            .build();
    }
}
