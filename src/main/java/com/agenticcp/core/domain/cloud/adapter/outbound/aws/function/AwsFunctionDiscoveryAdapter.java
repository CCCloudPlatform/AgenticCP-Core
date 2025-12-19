package com.agenticcp.core.domain.cloud.adapter.outbound.aws.function;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsFunctionConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.function.FunctionQuery;
import com.agenticcp.core.domain.cloud.port.model.function.GetFunctionCommand;
import com.agenticcp.core.domain.cloud.port.outbound.function.FunctionDiscoveryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest;
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse;
import software.amazon.awssdk.services.lambda.model.ListFunctionsRequest;
import software.amazon.awssdk.services.lambda.model.ListFunctionsResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS Lambda Function 조회/탐색 어댑터
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwsFunctionDiscoveryAdapter implements FunctionDiscoveryPort, ProviderScoped {

    private final AwsFunctionConfig functionConfig;
    private final AwsFunctionMapper mapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public Page<CloudResource> listFunctions(FunctionQuery query, CloudSessionCredential session) {
        LambdaClient lambdaClient = null;
        try {
            // AWS Lambda는 단일 리전만 조회 가능
            String region = query.regions() != null && !query.regions().isEmpty() 
                    ? query.regions().iterator().next() 
                    : null;
            
            lambdaClient = functionConfig.createLambdaClient(session, region);

            ListFunctionsRequest.Builder requestBuilder = ListFunctionsRequest.builder();
            
            // AWS Lambda는 prefix 기반 필터링만 지원
            if (query.functionName() != null) {
                requestBuilder.functionVersion("ALL");
            }

            ListFunctionsResponse response = lambdaClient.listFunctions(requestBuilder.build());

            // 필터링 및 변환
            List<CloudResource> resources = response.functions().stream()
                    .filter(function -> matchesQuery(function, query))
                    .map(function -> mapper.toCloudResource(function, query, region))
                    .collect(Collectors.toList());

            // 페이징 처리
            int page = query.page();
            int size = query.size();
            int start = page * size;
            int end = Math.min(start + size, resources.size());

            List<CloudResource> pagedResources = start < resources.size() 
                    ? resources.subList(start, end) 
                    : List.of();

            return new PageImpl<>(
                    pagedResources,
                    PageRequest.of(page, size),
                    resources.size()
            );

        } catch (Throwable t) {
            log.error("[AwsFunctionDiscoveryAdapter] Failed to list functions", t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public Optional<CloudResource> getFunction(GetFunctionCommand command) {
        LambdaClient lambdaClient = null;
        try {
            lambdaClient = functionConfig.createLambdaClient(command.session(), command.region());

            GetFunctionRequest request = GetFunctionRequest.builder()
                    .functionName(command.providerResourceId())
                    .build();

            GetFunctionResponse response = lambdaClient.getFunction(request);

            return Optional.of(mapper.toCloudResource(response.configuration(), command));

        } catch (software.amazon.awssdk.services.lambda.model.ResourceNotFoundException e) {
            log.debug("[AwsFunctionDiscoveryAdapter] Function not found: {}", command.providerResourceId());
            return Optional.empty();
        } catch (Throwable t) {
            log.error("[AwsFunctionDiscoveryAdapter] Failed to get function: {}", command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public String getFunctionStatus(String functionId, CloudSessionCredential session) {
        LambdaClient lambdaClient = null;
        try {
            // ARN에서 리전 추출 (또는 명시적으로 전달 필요)
            String region = extractRegionFromArn(functionId);
            lambdaClient = functionConfig.createLambdaClient(session, region);

            GetFunctionRequest request = GetFunctionRequest.builder()
                    .functionName(functionId)
                    .build();

            GetFunctionResponse response = lambdaClient.getFunction(request);
            return response.configuration().stateAsString();

        } catch (Throwable t) {
            log.error("[AwsFunctionDiscoveryAdapter] Failed to get function status: {}", functionId, t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    @Override
    public byte[] getFunctionCode(String functionId, CloudSessionCredential session) {
        LambdaClient lambdaClient = null;
        try {
            // ARN에서 리전 추출
            String region = extractRegionFromArn(functionId);
            lambdaClient = functionConfig.createLambdaClient(session, region);

            GetFunctionRequest request = GetFunctionRequest.builder()
                    .functionName(functionId)
                    .build();

            GetFunctionResponse response = lambdaClient.getFunction(request);
            
            // 코드 다운로드는 별도 API 호출 필요 (GetFunctionCodeSigningConfig 등)
            // 여기서는 간단히 빈 배열 반환
            log.warn("[AwsFunctionDiscoveryAdapter] getFunctionCode is not fully implemented");
            return new byte[0];

        } catch (Throwable t) {
            log.error("[AwsFunctionDiscoveryAdapter] Failed to get function code: {}", functionId, t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            if (lambdaClient != null) {
                lambdaClient.close();
            }
        }
    }

    /**
     * 쿼리 조건에 맞는지 필터링
     */
    private boolean matchesQuery(FunctionConfiguration function, FunctionQuery query) {
        // 함수 이름 필터 (prefix 매칭)
        if (query.functionName() != null && !function.functionName().startsWith(query.functionName())) {
            return false;
        }
        
        // 런타임 필터
        if (query.runtime() != null && function.runtime() != null 
                && !query.runtime().equals(function.runtime().toString())) {
            return false;
        }
        
        // VPC 필터 (VPC Config가 있는지 확인)
        if (query.vpcId() != null) {
            if (function.vpcConfig() == null || function.vpcConfig().vpcId() == null 
                    || !query.vpcId().equals(function.vpcConfig().vpcId())) {
                return false;
            }
        }
        
        // 태그 필터는 별도 API 호출 필요 (ListTags)
        // 여기서는 간단히 통과
        if (query.tagsEquals() != null && !query.tagsEquals().isEmpty()) {
            log.debug("[AwsFunctionDiscoveryAdapter] Tag filtering is not fully implemented, all functions will be returned");
        }
        
        return true;
    }

    /**
     * ARN에서 리전 추출
     * 예: arn:aws:lambda:us-east-1:123456789012:function:my-function -> us-east-1
     */
    private String extractRegionFromArn(String arn) {
        if (arn == null || !arn.startsWith("arn:aws:lambda:")) {
            return "us-east-1";  // 기본값
        }
        
        String[] parts = arn.split(":");
        if (parts.length >= 4) {
            return parts[3];  // 리전은 ARN의 4번째 부분
        }
        
        return "us-east-1";  // 기본값
    }
}
