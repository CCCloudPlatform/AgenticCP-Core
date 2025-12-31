package com.agenticcp.core.domain.cloud.adapter.outbound.aws.function;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsFunctionConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionInvokeCommand;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionInvocationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * AWS Lambda Function 실행 어댑터
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsFunctionInvocationAdapter implements FunctionInvocationPort, ProviderScoped {

    private final AwsFunctionConfig functionConfig;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public String invokeFunction(FunctionInvokeCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            InvokeRequest.Builder requestBuilder = InvokeRequest.builder()
                    .functionName(command.providerResourceId())
                    .payload(software.amazon.awssdk.core.SdkBytes.fromUtf8String(command.payload()));

            // InvocationType 설정
            InvocationType invocationType = mapInvocationType(command.invocationType());
            requestBuilder.invocationType(invocationType);

            // Qualifier 설정 (버전/별칭)
            if (command.qualifier() != null && !command.qualifier().isEmpty()) {
                requestBuilder.qualifier(command.qualifier());
            }

            InvokeResponse response = lambdaClient.invoke(requestBuilder.build());

            String result = response.payload().asUtf8String();
            log.info("[AwsFunctionInvocationAdapter] Successfully invoked function: {}, invocationType: {}", 
                    command.providerResourceId(), command.invocationType());
            
            return result;

        } catch (Throwable t) {
            log.error("[AwsFunctionInvocationAdapter] Failed to invoke function: {}", command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public String invokeFunctionAsync(FunctionInvokeCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            InvokeRequest.Builder requestBuilder = InvokeRequest.builder()
                    .functionName(command.providerResourceId())
                    .payload(software.amazon.awssdk.core.SdkBytes.fromUtf8String(command.payload()))
                    .invocationType(InvocationType.EVENT);  // 비동기 실행

            // Qualifier 설정
            if (command.qualifier() != null && !command.qualifier().isEmpty()) {
                requestBuilder.qualifier(command.qualifier());
            }

            InvokeResponse response = lambdaClient.invoke(requestBuilder.build());

            // 비동기 실행의 경우 RequestId 반환
            String requestId = response.responseMetadata() != null 
                    ? response.responseMetadata().requestId() 
                    : "unknown";
            
            log.info("[AwsFunctionInvocationAdapter] Successfully invoked function async: {}, requestId: {}", 
                    command.providerResourceId(), requestId);
            
            return requestId;

        } catch (Throwable t) {
            log.error("[AwsFunctionInvocationAdapter] Failed to invoke function async: {}", command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    /**
     * invocationType 문자열 → AWS SDK InvocationType 변환
     */
    private InvocationType mapInvocationType(String invocationType) {
        if (invocationType == null || invocationType.isEmpty()) {
            return InvocationType.REQUEST_RESPONSE;
        }

        return switch (invocationType.toUpperCase()) {
            case "EVENT" -> InvocationType.EVENT;
            case "DRYRUN" -> InvocationType.DRY_RUN;
            case "REQUESTRESPONSE" -> InvocationType.REQUEST_RESPONSE;
            default -> InvocationType.REQUEST_RESPONSE;
        };
    }
}
