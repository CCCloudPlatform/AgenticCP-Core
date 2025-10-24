import org.springframework.stereotype.Component;

import com.agenticcp.core.domain.cloud.entity.CloudResource;

import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;


@Component
public class AwsVpcMapper {
    // 응답 모델 
   public CloudResource toCloudResource(CreateVpcResponse response) {
    return CloudResource.builder()
        .resourceName(response.vpc().vpcId())
        .metadata(response.toString())
        .build();
   }
}
