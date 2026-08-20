package com.mutsapifa.mcmmuse.catalog.presentation;

import com.mutsapifa.mcmmuse.catalog.application.McmProductService;
import com.mutsapifa.mcmmuse.catalog.presentation.dto.McmProductPageResponse;
import com.mutsapifa.mcmmuse.catalog.presentation.dto.McmProductResponse;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 계약 §2 — home 상품보기 · 제품상세 (화면 7-a/b/c). */
@RestController
public class McmProductController {

  private final McmProductService mcmProductService;

  public McmProductController(McmProductService mcmProductService) {
    this.mcmProductService = mcmProductService;
  }

  /**
   * §2-1 — page 없이 호출하면 기존 전체 배열(하위호환), page를 주면 페이지 봉투 응답. size는 기본 8, 1~50으로 보정.
   */
  @GetMapping("/api/v1/mcm-products")
  public Object list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Category category,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false, defaultValue = "8") int size) {
    if (page == null) {
      return mcmProductService.list(query, category).stream()
          .map(McmProductResponse::from)
          .toList();
    }
    int safeSize = Math.min(Math.max(1, size), 50);
    return McmProductPageResponse.from(
        mcmProductService.listPaged(query, category, page, safeSize));
  }

  @GetMapping("/api/v1/mcm-products/{id}")
  public McmProductResponse detail(@PathVariable Long id) {
    return McmProductResponse.from(mcmProductService.get(id));
  }
}
