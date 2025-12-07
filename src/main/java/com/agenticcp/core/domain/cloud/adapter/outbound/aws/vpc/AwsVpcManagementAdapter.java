package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vpc;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.vpc.CreateVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.DeleteVpcCommand;
import com.agenticcp.core.domain.cloud.port.model.vpc.GetVpcCommand;
import com.agenticcp.core.domain.cloud.dto.ListVpcsQueryRequest;
import com.agenticcp.core.domain.cloud.port.model.vpc.UpdateVpcCommand;
import com.agenticcp.core.domain.cloud.port.outbound.vpc.VpcManagementPort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * AWS VPC 관리 어댑터
 * 
 * AWS SDK를 사용하여 VPC 관리 기능을 제공하는 어댑터
 * Usecase에서 전달받은 세션 자격증명만을 사용하여 AWS SDK를 호출합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AwsVpcManagementAdapter implements VpcManagementPort, ProviderScoped {

    private final AwsClientConfig awsClientConfig;
    private final AwsVpcMapper awsVpcMapper;

    @Override
    public CloudResource createVpc(CreateVpcCommand command) {
        try (Ec2Client ec2Client = awsClientConfig.createEc2Client(command.session(), command.region())) {
            CreateVpcRequest createVpcRequest = CreateVpcRequest.builder()
                .cidrBlock(command.cidrBlock())
                .build();
            
            CreateVpcResponse createVpcResponse = ec2Client.createVpc(createVpcRequest);
            
            // 태그 추가 (VPC 이름 및 기타 태그)
            if ((command.vpcName() != null && !command.vpcName().isEmpty()) || 
                (command.tags() != null && !command.tags().isEmpty())) {
                List<Tag> tags = new java.util.ArrayList<>();
                
                if (command.vpcName() != null && !command.vpcName().isEmpty()) {
                    tags.add(Tag.builder().key("Name").value(command.vpcName()).build());
                }
                
                if (command.tags() != null) {
                    command.tags().forEach((key, value) -> 
                        tags.add(Tag.builder().key(key).value(value).build())
                    );
                }
                
                if (!tags.isEmpty()) {
                    CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                        .resources(createVpcResponse.vpc().vpcId())
                        .tags(tags)
                        .build();
                    ec2Client.createTags(createTagsRequest);
                }
            }
            
            return awsVpcMapper.toCloudResource(createVpcResponse.vpc(), command);
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public Optional<CloudResource> getVpc(GetVpcCommand command) {
        try (Ec2Client ec2Client = awsClientConfig.createEc2Client(command.session(), command.region())) {
            DescribeVpcsRequest request = DescribeVpcsRequest.builder()
                .vpcIds(command.providerResourceId())
                .build();
            DescribeVpcsResponse response = ec2Client.describeVpcs(request);
            
            if (response.vpcs().isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(awsVpcMapper.toCloudResource(response.vpcs().get(0), command));
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public List<CloudResource> listVpcs(ListVpcsQueryRequest query) {
        try (Ec2Client ec2Client = awsClientConfig.createEc2Client(query.session(), query.region())) {
            DescribeVpcsRequest.Builder requestBuilder = DescribeVpcsRequest.builder();
            List<Filter> filters = new java.util.ArrayList<>();
            
            // 필터 조건 추가
            if (query.vpcName() != null && !query.vpcName().isEmpty()) {
                filters.add(Filter.builder()
                    .name("tag:Name")
                    .values(query.vpcName())
                    .build());
            }
            
            if (query.cidrBlock() != null && !query.cidrBlock().isEmpty()) {
                filters.add(Filter.builder()
                    .name("cidr-block")
                    .values(query.cidrBlock())
                    .build());
            }
            
            // 태그 필터 추가
            if (query.tags() != null && !query.tags().isEmpty()) {
                query.tags().forEach((key, value) -> {
                    filters.add(Filter.builder()
                        .name("tag:" + key)
                        .values(value)
                        .build());
                });
            }
            
            if (!filters.isEmpty()) {
                requestBuilder.filters(filters);
            }
            
            DescribeVpcsResponse response = ec2Client.describeVpcs(requestBuilder.build());
            
            return response.vpcs().stream()
                .map(vpc -> awsVpcMapper.toCloudResource(vpc, query))
                .toList();
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public CloudResource updateVpc(UpdateVpcCommand command) {
        try (Ec2Client ec2Client = awsClientConfig.createEc2Client(command.session(), command.region())) {
            // AWS VPC는 직접적인 업데이트 API가 없으므로 태그 업데이트로 처리
            if (command.tags() != null && !command.tags().isEmpty()) {
                List<Tag> tags = command.tags().entrySet().stream()
                    .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build())
                    .toList();
                
                CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                    .resources(command.providerResourceId())
                    .tags(tags)
                    .build();
                ec2Client.createTags(createTagsRequest);
            }
            
            // VPC 이름 업데이트 (태그로 처리)
            if (command.vpcName() != null && !command.vpcName().isEmpty()) {
                CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                    .resources(command.providerResourceId())
                    .tags(Tag.builder().key("Name").value(command.vpcName()).build())
                    .build();
                ec2Client.createTags(createTagsRequest);
            }
            
            // 업데이트된 VPC 정보 조회
            GetVpcCommand getCommand = GetVpcCommand.builder()
                .providerType(command.providerType())
                .accountScope(command.accountScope())
                .region(command.region())
                .providerResourceId(command.providerResourceId())
                .serviceKey(null)
                .resourceType(null)
                .session(command.session())
                .build();
            
            return getVpc(getCommand).orElseThrow(() -> 
                new RuntimeException("VPC not found after update: " + command.providerResourceId()));
        } catch (Throwable e) {
            throw CloudErrorTranslator.translate(e);
        }
    }

    @Override
    public void deleteVpc(DeleteVpcCommand command) {
        try (Ec2Client ec2Client = awsClientConfig.createEc2Client(command.session(), command.region())) {
            DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(command.providerResourceId())
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
