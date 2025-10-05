package com.agenticcp.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.redis.enabled=false",
        "spring.cache.type=simple"
})
@ActiveProfiles("test")
class AgenticCpCoreApplicationTests {

    @Test
    void contextLoads() {
        // 애플리케이션 컨텍스트가 정상적으로 로드되는지 테스트
        // Redis가 비활성화된 상태에서도 정상 동작하는지 확인
    }
}
