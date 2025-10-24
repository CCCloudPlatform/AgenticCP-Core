
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VpcCreateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VpcManagementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;


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
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
