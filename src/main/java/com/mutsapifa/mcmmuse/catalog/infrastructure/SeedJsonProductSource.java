package com.mutsapifa.mcmmuse.catalog.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutsapifa.mcmmuse.catalog.application.ProductSource;
import com.mutsapifa.mcmmuse.catalog.application.SeedProduct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** classpath:seed/mcm-products.json — scripts/csv_to_seed.py 생성물. */
@Component
public class SeedJsonProductSource implements ProductSource {

  private static final String SEED_PATH = "seed/mcm-products.json";

  private final ObjectMapper objectMapper;

  public SeedJsonProductSource(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public List<SeedProduct> fetch() {
    ClassPathResource resource = new ClassPathResource(SEED_PATH);
    if (!resource.exists()) {
      return List.of();
    }
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, new TypeReference<List<SeedProduct>>() {});
    } catch (IOException e) {
      throw new UncheckedIOException("시드 파일 파싱 실패: " + SEED_PATH, e);
    }
  }
}
