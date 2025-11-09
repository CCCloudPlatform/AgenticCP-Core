package com.agenticcp.core;

import com.agenticcp.core.domain.cloud.adapter.outbound.mock.MockAdaptersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "app.redis.enabled=false",
        "spring.cache.type=simple",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "logging.level.org.springframework.web=WARN",
        "logging.level.org.hibernate=WARN",
        "logging.level.org.springframework.boot.autoconfigure=WARN",
        "logging.level.org.springframework.context=WARN",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration",
        "security.jwt.secret=ZmFrZV9zZWNyZXRfZm9yX2Rldl9vbmx5X3VzZV9jaGFuZ2VfbWU=",
        "config.cipher.key=MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA="
})
@ActiveProfiles("test")
@Import(MockAdaptersConfig.class)
class AgenticCpCoreApplicationTests {

    @DynamicPropertySource
    static void injectTestCipherKey(DynamicPropertyRegistry registry) {
        registry.add("config.cipher.key", () -> "MDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDA=");
        // 필요 시 키 누락 동작을 READ_ONLY로 강제하려면 아래 주석 해제
        // registry.add("config.cipher.missingKeyBehavior", () -> "READ_ONLY");
    }

    @Test
    void contextLoads() {
        // 애플리케이션 컨텍스트가 정상적으로 로드되는지 테스트
        // Redis가 비활성화된 상태에서도 정상 동작하는지 확인
    }
}
 