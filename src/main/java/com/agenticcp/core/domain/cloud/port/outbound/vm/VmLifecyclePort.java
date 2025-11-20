package com.agenticcp.core.domain.cloud.port.outbound.vm;

import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;

/**
 * VM 생명주기 관리 책임 포트
 */
public interface VmLifecyclePort {

    String createInstance(VmCreateCommand command);

    void startInstance(String instanceId);

    void stopInstance(String instanceId);

    void rebootInstance(String instanceId);

    void terminateInstance(String instanceId);

    void deleteInstance(VmDeleteCommand command);

    void updateInstance(VmUpdateCommand command);
}

