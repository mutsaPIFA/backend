package com.mutsapifa.mcmmuse.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 {@code app.demo.*} 바인딩 — 시연(게스트 발급) 설정.
 *
 * <p>guestTemplateEmail: 게스트 시드 옷장의 원본 계정. 비어 있으면 게스트는 빈 옷장으로 시작한다.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(String guestTemplateEmail) {}
