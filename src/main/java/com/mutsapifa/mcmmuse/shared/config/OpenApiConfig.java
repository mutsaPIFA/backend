package com.mutsapifa.mcmmuse.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 설정 — Authorize 버튼에 Bearer 토큰을 넣어 보호 엔드포인트를 테스트할 수 있게 한다.
 *
 * <p>사용법: /api/v1/auth/login 응답의 accessToken → 우상단 Authorize에 붙여넣기.
 *
 * <p>⚠️ Swagger는 "실행 가능한 확인"용이다. 계약의 단일 소스는 docs 레포 api-v1.md — 엔드포인트별 설명 어노테이션을 여기에 쌓지 않는다.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "bearerAuth";

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("MCM MUSE API")
                .version("v1")
                .description("계약 기준: mutsaPIFA/docs의 api-v1.md"))
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
