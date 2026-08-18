package com.mutsapifa.mcmmuse.catalog.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 상품 적재 — sku 기준 upsert(멱등). row 삭제 금지(옷장·룩 FK), 원천에서 빠진 상품은 active=false (DB-컨벤션). */
@Slf4j
@Service
public class McmProductIngestService {

  private final ProductSource productSource;
  private final McmProductRepository mcmProductRepository;

  public McmProductIngestService(
      ProductSource productSource, McmProductRepository mcmProductRepository) {
    this.productSource = productSource;
    this.mcmProductRepository = mcmProductRepository;
  }

  @Transactional
  public IngestSummary ingest() {
    List<SeedProduct> seeds = productSource.fetch();
    if (seeds.isEmpty()) {
      log.warn("시드가 비어 있어 적재를 건너뜁니다");
      return new IngestSummary(0, 0, 0);
    }

    Map<String, McmProduct> existing =
        mcmProductRepository.findAll().stream()
            .collect(Collectors.toMap(McmProduct::getSku, Function.identity()));
    Set<String> seedSkus = seeds.stream().map(SeedProduct::sku).collect(Collectors.toSet());

    int inserted = 0;
    int updated = 0;
    for (SeedProduct s : seeds) {
      McmProduct found = existing.get(s.sku());
      if (found == null) {
        mcmProductRepository.save(
            new McmProduct(
                s.sku(),
                s.name(),
                s.category(),
                s.color(),
                s.material(),
                s.price(),
                s.imageUrl(),
                s.productUrl(),
                s.description(),
                s.size(),
                s.imageUrls(),
                s.mood(),
                s.styleNote()));
        inserted++;
      } else {
        found.updateFrom(
            s.name(),
            s.category(),
            s.color(),
            s.material(),
            s.price(),
            s.imageUrl(),
            s.productUrl(),
            s.description(),
            s.size(),
            s.imageUrls(),
            s.mood(),
            s.styleNote());
        updated++;
      }
    }

    // 원천에서 사라진 상품 비활성 (row 삭제 금지 — 옷장·룩이 참조)
    int deactivated = 0;
    for (McmProduct p : existing.values()) {
      if (!seedSkus.contains(p.getSku()) && p.isActive()) {
        p.deactivate();
        deactivated++;
      }
    }

    log.info("상품 적재 완료 — 신규 {}, 갱신 {}, 비활성 {}", inserted, updated, deactivated);
    return new IngestSummary(inserted, updated, deactivated);
  }

  public record IngestSummary(int inserted, int updated, int deactivated) {}
}
