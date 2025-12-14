package com.agenticcp.core.domain.cloud.port.model.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 생성 도메인 커맨드 (CSP 중립적)
 *
 * UseCase Service에서 Adapter로 전달되는 내부 명령 모델입니다.
 * CSP 중립적인 필드를 사용하며, 각 CSP Adapter의 Mapper에서 CSP 특화 요청으로 변환합니다.
 * 멀티 테넌트 환경에서 자격증명 격리를 위해 세션 정보를 포함합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Getter
@Builder
public class VmCreateCommand {

    /**
     * VM 이미지 식별자 (CSP별로 해석)
     * - AWS: AMI ID
     * - GCP: Image family/project
     * - Azure: Image reference
     */
    private final String image;
    
    /**
     * 인스턴스 크기/타입 (CSP별로 매핑)
     * - AWS: t2.micro, t3.small 등
     * - GCP: n1-standard-1 등
     * - Azure: Standard_DS1_v2 등
     */
    private final String instanceSize;
    
    /**
     * SSH 키 이름 또는 공개 키
     */
    private final String sshKey;
    
    /**
     * 네트워크/보안 그룹 식별자
     */
    private final String networkSecurityId;
    
    /**
     * 서브넷 식별자
     */
    private final String subnetId;
    
    /**
     * 리전/가용영역
     */
    private final String zone;
    
    /**
     * 사용자 데이터 (스크립트 또는 클라우드-초기화 데이터)
     */
    private final String userData;
    
    /**
     * 인스턴스에 적용할 태그/라벨
     */
    private final Map<String, String> tags;
    
    /**
     * 최소 생성 인스턴스 수
     */
    @Builder.Default
    private int minCount = 1;
    
    /**
     * 최대 생성 인스턴스 수
     */
    @Builder.Default
    private int maxCount = 1;

    /**
     * 세션 자격증명
     * UseCase Service에서 획득하여 주입합니다.
     */
    private final CloudSessionCredential session;
}
