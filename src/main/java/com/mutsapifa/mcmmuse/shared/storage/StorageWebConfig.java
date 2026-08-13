package com.mutsapifa.mcmmuse.shared.storage;

import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** {@code GET /images/**} → 로컬 업로드 디렉토리 정적 서빙 (인증 제외 경로 — SecurityConfig 참조). */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class StorageWebConfig implements WebMvcConfigurer {

  private final StorageProperties properties;

  public StorageWebConfig(StorageProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = Path.of(properties.localPath()).toAbsolutePath().toUri().toString();
    registry.addResourceHandler("/images/**").addResourceLocations(location);
  }
}
