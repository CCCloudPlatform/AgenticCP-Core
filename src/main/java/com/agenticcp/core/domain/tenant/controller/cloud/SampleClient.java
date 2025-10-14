package com.agenticcp.core.domain.tenant.controller.cloud;

import com.agenticcp.core.domain.tenant.controller.cloud.dto.CloudResourceResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "",
        url = ""
)
public interface SampleClient {

    @PostMapping("/api/{samplePathVariable}")
    CloudResourceResult createVpc(
            @PathVariable String samplePathVariable,
            @RequestBody String sampleRequestBody
    );
}
