package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.vm.VmCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.vm.VmUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

/**
 * AWS VM 생명주기 어댑터
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmLifecycleAdapter implements VmLifecyclePort, ProviderScoped {

    private final Ec2Client ec2Client;
    private final AwsVmMapper mapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public String createInstance(VmCreateCommand command) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Creating VM instance with command: {}", command);

            RunInstancesRequest awsRequest = mapper.toRunInstancesRequest(command);
            RunInstancesResponse response = ec2Client.runInstances(awsRequest);

            String instanceId = response.instances().get(0).instanceId();
            log.info("[AwsVmLifecycleAdapter] Successfully created VM instance: {}", instanceId);

            return instanceId;

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to create VM instance", t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void startInstance(String instanceId) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Starting VM instance: {}", instanceId);

            StartInstancesRequest request = StartInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2Client.startInstances(request);
            log.info("[AwsVmLifecycleAdapter] Successfully started VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to start VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void stopInstance(String instanceId) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Stopping VM instance: {}", instanceId);

            StopInstancesRequest request = StopInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2Client.stopInstances(request);
            log.info("[AwsVmLifecycleAdapter] Successfully stopped VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to stop VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void rebootInstance(String instanceId) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Rebooting VM instance: {}", instanceId);

            RebootInstancesRequest request = RebootInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2Client.rebootInstances(request);
            log.info("[AwsVmLifecycleAdapter] Successfully rebooted VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to reboot VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void terminateInstance(String instanceId) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Terminating VM instance: {}", instanceId);

            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2Client.terminateInstances(request);
            log.info("[AwsVmLifecycleAdapter] Successfully terminated VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to terminate VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void deleteInstance(VmDeleteCommand command) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Deleting VM instance: {}", command.getInstanceId());

            TerminateInstancesRequest request = mapper.toTerminateInstancesRequest(command);
            ec2Client.terminateInstances(request);

            log.info("[AwsVmLifecycleAdapter] Successfully deleted VM instance: {}", command.getInstanceId());

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to delete VM instance: {}", command.getInstanceId(), t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void updateInstance(VmUpdateCommand command) {
        try {
            log.debug("[AwsVmLifecycleAdapter] Updating VM instance: {}", command.getInstanceId());

            ModifyInstanceAttributeRequest request = mapper.toModifyInstanceAttributeRequest(command);
            ec2Client.modifyInstanceAttribute(request);

            log.info("[AwsVmLifecycleAdapter] Successfully updated VM instance: {}", command.getInstanceId());

        } catch (Throwable t) {
            log.error("[AwsVmLifecycleAdapter] Failed to update VM instance: {}", command.getInstanceId(), t);
            throw CloudErrorTranslator.translate(t);
        }
    }
}

