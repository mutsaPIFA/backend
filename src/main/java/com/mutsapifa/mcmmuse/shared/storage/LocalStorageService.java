package com.mutsapifa.mcmmuse.shared.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 로컬 디스크 저장 (데모용). {@code GET /images/**} 정적 서빙은 {@link StorageWebConfig}. */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

  private final StorageProperties properties;

  public LocalStorageService(StorageProperties properties) {
    this.properties = properties;
  }

  @Override
  public String store(byte[] data, String prefix, String extension) {
    String key = prefix + "/" + UUID.randomUUID() + "." + extension;
    Path target = Path.of(properties.localPath()).resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, data);
    } catch (IOException e) {
      throw new UncheckedIOException("이미지 저장 실패: " + key, e);
    }
    return key;
  }

  @Override
  public String resolveUrl(String key) {
    return properties.publicBaseUrl() + "/" + key;
  }
}
