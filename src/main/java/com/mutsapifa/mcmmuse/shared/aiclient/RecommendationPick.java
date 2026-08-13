package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/** 추천 1건 — 상품 id 제안 + 이유. 실재 여부는 호출부(styling)가 DB로 재검증한다. */
public record RecommendationPick(Long productId, String reason, List<Long> pairsWithItemIds) {}
