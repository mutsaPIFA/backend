package com.mutsapifa.mcmmuse.shared.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 {@code app.cors.*} 바인딩. (@Value 는 YAML 리스트를 못 읽는다) */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {}
