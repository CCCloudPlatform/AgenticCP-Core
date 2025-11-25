package com.agenticcp.core.domain.cloud.port.outbound.storage;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.storage.CreateObjectStorageContainerCommand;
import com.agenticcp.core.domain.cloud.port.model.storage.UpdateObjectStorageContainerCommand;

/**
 * Object Storage Container 관리 포트 - Object Storage Container 생성, 수정, 삭제 기능을 정의하는 계약
 * 
 * Object Storage Container는 일반적인 컴퓨팅 리소스와 달리 start/stop/terminate 생명주기가 없으므로
 * 별도의 관리 포트로 분리하여 명확한 역할을 부여합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface ObjectStorageManagementPort {

    /**
     * Object Storage Container를 생성합니다.
     *
     * @param command 생성 명령 (세션 포함)
     * @return 생성된 CloudResource
     * @throws com.agenticcp.core.common.exception.BusinessException Container 이름 중복, 권한 없음, 잘못된 이름 형식
     */
    CloudResource createContainer(CreateObjectStorageContainerCommand command);

    /**
     * Object Storage Container 설정을 업데이트합니다.
     *
     * @param command 업데이트 명령 (세션 포함)
     * @return 업데이트된 CloudResource
     * @throws com.agenticcp.core.common.exception.BusinessException Container 없음, 권한 없음
     */
    CloudResource updateContainer(UpdateObjectStorageContainerCommand command);

    /**
     * Object Storage Container를 삭제합니다.
     * 
     * @param containerName Container 이름
     * @param session 클라우드 세션 자격증명
     * @throws com.agenticcp.core.common.exception.BusinessException Container 없음, 권한 없음, Container가 비어있지 않음
     */
    void deleteContainer(CloudSessionCredential session, String containerName);

    /**
     * Object Storage Container를 강제 삭제합니다 (내용물 포함).
     * 
     * @param containerName Container 이름
     * @param session 클라우드 세션 자격증명
     * @throws com.agenticcp.core.common.exception.BusinessException Container 없음, 권한 없음
     */
    void forceDeleteContainer(String containerName, CloudSessionCredential session);
}
