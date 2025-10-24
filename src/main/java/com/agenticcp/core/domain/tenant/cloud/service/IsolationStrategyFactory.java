package com.agenticcp.core.domain.tenant.cloud.service;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class IsolationStrategyFactory {

    private final Map<TenantIsolation.IsolationLevel, IsolationStrategy> strategies;

    // 빠른 조회를 위해 생성자에서 맵을 만듦
    public IsolationStrategyFactory(List<IsolationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .flatMap(this::findSupportedLevel)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    // 격리 수준 조회
    public IsolationStrategy getStrategy(TenantIsolation.IsolationLevel isolationLevel) {
        return strategies.get(isolationLevel);
    }

    private Stream<Map.Entry<TenantIsolation.IsolationLevel, IsolationStrategy>> findSupportedLevel(IsolationStrategy st) {
        return Arrays.stream(TenantIsolation.IsolationLevel.values())
                .filter(st::supports)
                .map(level -> Map.entry(level, st));
    }

}
