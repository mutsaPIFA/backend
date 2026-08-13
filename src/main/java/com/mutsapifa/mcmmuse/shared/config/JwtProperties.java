package com.mutsapifa.mcmmuse.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 {@code app.jwt.*} 바인딩. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret, Duration accessTokenTtl, Duration refreshTokenTtl, boolean cookieSecure) {}
