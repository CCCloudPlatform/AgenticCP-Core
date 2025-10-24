package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.port.model.CostInfo;
import com.agenticcp.core.domain.cloud.port.model.CostQuery;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import org.springframework.data.domain.Page;

public interface ResourceCostPort {
    CostInfo getCost(ResourceIdentity id);
    Page<CostInfo> listCosts(CostQuery query);
}
