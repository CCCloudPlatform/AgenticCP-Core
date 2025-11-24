package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import java.util.function.Supplier;

public class ResilientPortExecutor {

    // TODO: Resilience4j 의존성 추가 후 실제 구현
    public static <T> T execute(Supplier<T> supplier) {
        return supplier.get();
    }
}
