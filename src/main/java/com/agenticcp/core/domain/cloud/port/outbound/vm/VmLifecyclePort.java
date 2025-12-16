package com.agenticcp.core.domain.cloud.port.outbound.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;

/**
 * VM 생명주기 관리 책임 포트
 * 
 * <p>모든 Management 작업은 세션 자격증명을 명시적으로 전달받습니다.</p>
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
public interface VmLifecyclePort {

    String createInstance(VmCreateCommand command);

    void startInstance(String instanceId, CloudSessionCredential session);

    void stopInstance(String instanceId, CloudSessionCredential session);

    void rebootInstance(String instanceId, CloudSessionCredential session);

    void terminateInstance(String instanceId, CloudSessionCredential session);

    void deleteInstance(VmDeleteCommand command);

    void updateInstance(VmUpdateCommand command);
}

