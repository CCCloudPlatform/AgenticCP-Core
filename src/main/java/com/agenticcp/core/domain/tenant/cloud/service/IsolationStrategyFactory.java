package com.agenticcp.core.domain.tenant.cloud.service;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IsolationStrategyFactory {

    private final Map<TenantIsolation.IsolationLevel, IsolationStrategy> strategies;

    public IsolationStrategyFactory(List<IsolationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> findSupportedLevel(strategy),
                        Function.identity()
                ));
    }

    public IsolationStrategy getStrategy(TenantIsolation.IsolationLevel isolationLevel) {
        return strategies.get(isolationLevel);
    }

    private TenantIsolation.IsolationLevel findSupportedLevel(IsolationStrategy st) {
        return Arrays.stream(TenantIsolation.IsolationLevel.values())
                .filter(st::supports)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Isolation strategy does not support any isolation level"));
    }

}
