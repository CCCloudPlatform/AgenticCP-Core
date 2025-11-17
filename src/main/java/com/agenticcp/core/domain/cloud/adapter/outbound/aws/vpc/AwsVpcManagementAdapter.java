package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vpc;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VpcUpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;
import com.agenticcp.core.domain.cloud.port.model.VpcQuery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Optional;

/**
 * AWS VPC 관리 어댑터
 * 
 * AWS SDK를 사용하여 VPC 관리 기능을 제공하는 어댑터
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AwsVpcManagementAdapter implements VpcManagementPort, ProviderScoped {

    private final Ec2Client ec2Client;
    private final AwsVpcMapper awsVpcMapper;

    @Override
    public CloudResource createVpc(VpcCreateRequest request) {
        try {
            CreateVpcRequest createVpcRequest = CreateVpcRequest.builder()
                .cidrBlock(request.getCidrBlock())
                .build();
            CreateVpcResponse createVpcResponse = ec2Client.createVpc(createVpcRequest);

            return awsVpcMapper.toCloudResource(createVpcResponse);
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public Optional<CloudResource> getVpc(ResourceIdentity vpcId) {
        try {
            DescribeVpcsRequest request = DescribeVpcsRequest.builder()
                .vpcIds(vpcId.getProviderResourceId())
                .build();
            DescribeVpcsResponse response = ec2Client.describeVpcs(request);
            
            if (response.vpcs().isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(awsVpcMapper.toCloudResource(response.vpcs().get(0)));
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public List<CloudResource> listVpcs(VpcQuery query) {
        try {
            DescribeVpcsRequest.Builder requestBuilder = DescribeVpcsRequest.builder();
            
            // 필터 조건 추가
            if (query.getVpcName() != null) {
                requestBuilder.filters(Filter.builder()
                    .name("tag:Name")
                    .values(query.getVpcName())
                    .build());
            }
            
            if (query.getCidrBlock() != null) {
                requestBuilder.filters(Filter.builder()
                    .name("cidr")
                    .values(query.getCidrBlock())
                    .build());
            }
            
            DescribeVpcsResponse response = ec2Client.describeVpcs(requestBuilder.build());
            
            return response.vpcs().stream()
                .map(awsVpcMapper::toCloudResource)
                .toList();
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public CloudResource updateVpc(ResourceIdentity vpcId, VpcUpdateRequest request) {
        try {
            // AWS VPC는 직접적인 업데이트 API가 없으므로 태그 업데이트로 처리
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                    .resources(vpcId.getProviderResourceId())
                    .tags(request.getTags().entrySet().stream()
                        .map(entry -> Tag.builder()
                            .key(entry.getKey())
                            .value(entry.getValue())
                            .build())
                        .toList())
                    .build();
                ec2Client.createTags(createTagsRequest);
            }
            
            // 업데이트된 VPC 정보 조회
            return getVpc(vpcId).orElseThrow(() -> 
                new RuntimeException("VPC not found after update"));
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public void deleteVpc(ResourceIdentity vpcId) {
        try {
            DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(vpcId.getProviderResourceId())
                .build();
            ec2Client.deleteVpc(request);
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
