package com.mutsapifa.mcmmuse.styling.application.dto;

import java.util.List;

/** 계약 §4-2 응답 모양 — {bestPick, more[]}. 재료 부족 시 bestPick=null 가능성은 없음(400 선처리). */
public record RecommendationResult(Item bestPick, List<Item> more) {

  public record Item(
      Long mcmProductId,
      String reason,
      List<Long> pairsWithItemIds,
      boolean isExpansion,
      ProductSummary product) {}

  /** 계약 §4-2 Recommendation.product — {id, name, imageUrl, price, productUrl} */
  public record ProductSummary(
      Long id, String name, String imageUrl, Integer price, String productUrl) {}
}
