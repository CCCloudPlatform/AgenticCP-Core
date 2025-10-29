package com.agenticcp.core.domain.organization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoveOrganizationRequest {
    @NotNull(message = "새로운 상위 조직 ID는 필수입니다")
    private Long newParentId;
}
