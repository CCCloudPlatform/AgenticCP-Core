package com.agenticcp.core.domain.cloud.port.outbound.cdn;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.cdn.CreateDistributionCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.DeleteDistributionCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.UpdateDistributionCommand;

/**
 * CDN Distribution 관리 포트
 * 
 * CDN Distribution의 생성, 수정, 삭제 기능을 정의합니다.
 * CloudFront Distribution은 start/stop 개념이 없으므로 별도의 관리 포트로 분리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface CDNManagementPort {
    
    /**
     * CDN Distribution을 생성합니다.
     * 
     * @param command 생성 명령 (세션, Origin, CacheBehavior, 태그 포함)
     * @return 생성된 CloudResource
     */
    CloudResource createDistribution(CreateDistributionCommand command);
    
    /**
     * CDN Distribution 설정을 업데이트합니다.
     * 
     * @param command 업데이트 명령 (세션, ETag, 설정 변경사항 포함)
     * @return 업데이트된 CloudResource
     */
    CloudResource updateDistribution(UpdateDistributionCommand command);
    
    /**
     * CDN Distribution을 삭제합니다.
     * Distribution은 삭제 전 반드시 비활성화(Disabled) 상태여야 합니다.
     * 
     * @param command 삭제 명령 (세션, Distribution ID, ETag 포함)
     */
    void deleteDistribution(DeleteDistributionCommand command);
}

