package com.mutsapifa.mcmmuse.catalog.presentation;

import com.mutsapifa.mcmmuse.catalog.application.McmProductService;
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

  @GetMapping("/api/v1/mcm-products")
  public List<McmProductResponse> list(
      @RequestParam(required = false) String query,
      @RequestParam(required = false) Category category) {
    return mcmProductService.list(query, category).stream().map(McmProductResponse::from).toList();
  }

  @GetMapping("/api/v1/mcm-products/{id}")
  public McmProductResponse detail(@PathVariable Long id) {
    return McmProductResponse.from(mcmProductService.get(id));
  }
}
