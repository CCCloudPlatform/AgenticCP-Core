package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AwsVmMapper 생성 기능 테스트
 */
class AwsVmMapperCreateTest {

    private AwsVmMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AwsVmMapper();
    }

    @Test
    void toRunInstancesRequest_기본요청() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        // When
        RunInstancesRequest result = mapper.toRunInstancesRequest(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.imageId()).isEqualTo("ami-12345678");
        assertThat(result.instanceType()).isEqualTo(InstanceType.T3_MICRO);
        assertThat(result.minCount()).isEqualTo(1);
        assertThat(result.maxCount()).isEqualTo(1);
        assertThat(result.keyName()).isNull();
        assertThat(result.securityGroupIds()).isEmpty();
        assertThat(result.subnetId()).isNull();
        assertThat(result.userData()).isNull();
        assertThat(result.tagSpecifications()).isEmpty();
    }

    @Test
    void toRunInstancesRequest_전체옵션요청() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.small")
            .keyName("my-key-pair")
            .securityGroupId("sg-12345678")
            .subnetId("subnet-12345678")
            .userData("#!/bin/bash\necho 'Hello World'")
            .tags(Map.of(
                "Environment", "Development",
                "Project", "TestProject"
            ))
            .minCount(2)
            .maxCount(3)
            .build();

        // When
        RunInstancesRequest result = mapper.toRunInstancesRequest(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.imageId()).isEqualTo("ami-12345678");
        assertThat(result.instanceType()).isEqualTo(InstanceType.T3_SMALL);
        assertThat(result.keyName()).isEqualTo("my-key-pair");
        assertThat(result.securityGroupIds()).containsExactly("sg-12345678");
        assertThat(result.subnetId()).isEqualTo("subnet-12345678");
        assertThat(result.userData()).isEqualTo("#!/bin/bash\necho 'Hello World'");
        assertThat(result.minCount()).isEqualTo(2);
        assertThat(result.maxCount()).isEqualTo(3);

        // 태그 확인
        assertThat(result.tagSpecifications()).hasSize(1);
        TagSpecification tagSpec = result.tagSpecifications().get(0);
        assertThat(tagSpec.resourceType()).isEqualTo(ResourceType.INSTANCE);
        assertThat(tagSpec.tags()).hasSize(2);
        
        // 태그 내용 확인
        assertThat(tagSpec.tags()).anyMatch(tag -> 
            tag.key().equals("Environment") && tag.value().equals("Development"));
        assertThat(tagSpec.tags()).anyMatch(tag -> 
            tag.key().equals("Project") && tag.value().equals("TestProject"));
    }

    @Test
    void toRunInstancesRequest_다양한인스턴스타입() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("m5.large")
            .minCount(1)
            .maxCount(1)
            .build();

        // When
        RunInstancesRequest result = mapper.toRunInstancesRequest(request);

        // Then
        assertThat(result.instanceType()).isEqualTo(InstanceType.M5_LARGE);
    }

    @Test
    void toRunInstancesRequest_빈태그() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .tags(Map.of()) // 빈 태그 맵
            .minCount(1)
            .maxCount(1)
            .build();

        // When
        RunInstancesRequest result = mapper.toRunInstancesRequest(request);

        // Then
        assertThat(result.tagSpecifications()).isEmpty();
    }

    @Test
    void toRunInstancesRequest_null태그() {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .imageId("ami-12345678")
            .instanceType("t3.micro")
            .tags(null) // null 태그
            .minCount(1)
            .maxCount(1)
            .build();

        // When
        RunInstancesRequest result = mapper.toRunInstancesRequest(request);

        // Then
        assertThat(result.tagSpecifications()).isEmpty();
    }
}
