package com.mutsapifa.mcmmuse.catalog.infrastructure;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McmProductRepository extends JpaRepository<McmProduct, Long> {

  /** 카탈로그 목록 — 항상 active 필터 (비활성은 옷장·룩에서만 보임) */
  List<McmProduct> findByActiveTrueOrderByIdAsc();

  List<McmProduct> findByActiveTrueAndCategoryOrderByIdAsc(Category category);

  /** 시드/피드 upsert 키 */
  Optional<McmProduct> findBySku(String sku);
}
