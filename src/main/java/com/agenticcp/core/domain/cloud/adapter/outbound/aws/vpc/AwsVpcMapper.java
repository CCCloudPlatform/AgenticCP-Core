package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vpc;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.Vpc;

/**
 * AWS VPC 응답을 CloudResource로 변환하는 매퍼
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
public class AwsVpcMapper {
    
    /**
     * CreateVpcResponse를 CloudResource로 변환
     */
    public CloudResource toCloudResource(CreateVpcResponse response) {
        return CloudResource.builder()
            .resourceName(response.vpc().vpcId())
            .metadata(response.toString())
            .build();
    }
    
    /**
     * Vpc를 CloudResource로 변환
     */
    public CloudResource toCloudResource(Vpc vpc) {
        return CloudResource.builder()
            .resourceName(vpc.vpcId())
            .metadata(vpc.toString())
            .build();
    }
}
