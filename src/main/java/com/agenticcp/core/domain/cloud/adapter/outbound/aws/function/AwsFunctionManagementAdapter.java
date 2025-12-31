package com.agenticcp.core.domain.cloud.adapter.outbound.aws.function;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsFunctionConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse;

/**
 * AWS Lambda Function 생명주기 관리 어댑터
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsFunctionManagementAdapter implements FunctionManagementPort, ProviderScoped {

    private final AwsFunctionConfig functionConfig;
    private final AwsFunctionMapper mapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public CloudResource createFunction(FunctionCreateCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            CreateFunctionRequest request = mapper.toCreateFunctionRequest(command);
            CreateFunctionResponse response = lambdaClient.createFunction(request);

            // 생성된 함수의 상세 정보 조회
            GetFunctionRequest getRequest = GetFunctionRequest.builder()
                    .functionName(command.functionName())
                    .build();
            GetFunctionResponse getResponse = lambdaClient.getFunction(getRequest);

            return mapper.toCloudResource(getResponse.configuration(), command);

        } catch (Throwable t) {
            log.error("[AwsFunctionManagementAdapter] Failed to create function: {}", command.functionName(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public CloudResource updateFunction(FunctionUpdateCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            // 코드 업데이트 (codeUri 또는 codeZip이 있는 경우)
            if (command.codeUri() != null || (command.codeZip() != null && command.codeZip().length > 0)) {
                UpdateFunctionCodeRequest.Builder codeRequestBuilder = UpdateFunctionCodeRequest.builder()
                        .functionName(command.providerResourceId());

                if (command.codeZip() != null && command.codeZip().length > 0) {
                    codeRequestBuilder.zipFile(software.amazon.awssdk.core.SdkBytes.fromByteArray(command.codeZip()));
                } else if (command.codeUri() != null) {
                    // S3 URI 파싱 및 설정
                    var s3Location = mapper.parseS3Uri(command.codeUri());
                    codeRequestBuilder.s3Bucket(s3Location.bucket())
                            .s3Key(s3Location.key());
                }

                lambdaClient.updateFunctionCode(codeRequestBuilder.build());
            }

            // 설정 업데이트
            UpdateFunctionConfigurationRequest.Builder configRequestBuilder = UpdateFunctionConfigurationRequest.builder()
                    .functionName(command.providerResourceId());

            if (command.runtime() != null) {
                configRequestBuilder.runtime(software.amazon.awssdk.services.lambda.model.Runtime.fromValue(command.runtime()));
            }
            if (command.handler() != null) {
                configRequestBuilder.handler(command.handler());
            }
            if (command.memorySize() != null) {
                configRequestBuilder.memorySize(command.memorySize());
            }
            if (command.timeout() != null) {
                configRequestBuilder.timeout(command.timeout());
            }
            if (command.roleArn() != null) {
                configRequestBuilder.role(command.roleArn());
            }
            if (command.description() != null) {
                configRequestBuilder.description(command.description());
            }
            if (command.environmentVariables() != null) {
                configRequestBuilder.environment(software.amazon.awssdk.services.lambda.model.Environment.builder()
                        .variables(command.environmentVariables())
                        .build());
            }

            UpdateFunctionConfigurationResponse response = lambdaClient.updateFunctionConfiguration(configRequestBuilder.build());

            // 업데이트된 함수 조회
            GetFunctionRequest getRequest = GetFunctionRequest.builder()
                    .functionName(command.providerResourceId())
                    .build();
            GetFunctionResponse getResponse = lambdaClient.getFunction(getRequest);

            // GetFunctionCommand 생성
            com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand getCommand = 
                    com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand.builder()
                            .providerType(command.providerType())
                            .accountScope(command.accountScope())
                            .region(command.region())
                            .providerResourceId(command.providerResourceId())
                            .serviceKey(null)
                            .resourceType(null)
                            .session(command.session())
                            .build();

            return mapper.toCloudResource(getResponse.configuration(), getCommand);

        } catch (Throwable t) {
            log.error("[AwsFunctionManagementAdapter] Failed to update function: {}", command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public void deleteFunction(FunctionDeleteCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            DeleteFunctionRequest request = DeleteFunctionRequest.builder()
                    .functionName(command.providerResourceId())
                    .build();

            lambdaClient.deleteFunction(request);
            log.info("[AwsFunctionManagementAdapter] Successfully deleted function: {}", command.providerResourceId());

        } catch (Throwable t) {
            log.error("[AwsFunctionManagementAdapter] Failed to delete function: {}", command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }
}
