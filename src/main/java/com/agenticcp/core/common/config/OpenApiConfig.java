package com.agenticcp.core.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgenticCP Core API")
                        .version("1.0.0")
                        .description("멀티 클라우드 플랫폼 통합 관리 API\n\n" +
                                "보안 정책:\n" +
                                "- ENCRYPTED 설정은 기본적으로 마스킹(***).\n" +
                                "- showSecret=true 요청은 관리자 권한(ROLE_ADMIN/ROLE_PLATFORM_ADMIN) 필요.\n" +
                                "- showSecret=true 응답은 Cache-Control/Pragma no-store/no-cache 적용.\n" +
                                "- 복호화 실패(PLATFORM_6017), 암호문 포맷 오류(PLATFORM_6019) 표준 에러코드 사용.")
                        .contact(new Contact()
                                .name("AgenticCP Team")
                                .email("support@agenticcp.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080/api").description("개발 서버"),
                        new Server().url("https://api.agenticcp.com").description("프로덕션 서버")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요. 예: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                        )
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("기본 인증 (사용자명/비밀번호)")
                        )
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API 키를 입력하세요")
                        )
                        .addSecuritySchemes("tenantKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Tenant-Key")
                                .description("요청 테넌트를 지정하는 헤더 (예: test-tenant)")
                        ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")
                        .addList("basicAuth")
                        .addList("apiKey")
                        .addList("tenantKey"));
    }
}
