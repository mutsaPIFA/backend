package com.mutsapifa.mcmmuse.shared.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 {@code app.storage.*} 바인딩. */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String type, String localPath, String publicBaseUrl) {}
