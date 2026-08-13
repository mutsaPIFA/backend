package com.mutsapifa.mcmmuse.catalog.application;

import java.util.List;

/**
 * 상품 데이터 원천 추상화 — 시드 JSON(현재) / 주최측 피드 / 크롤러(파킹랏)를 교체 가능하게.
 *
 * <p>갱신은 수동: 브라우저 재수확 → {@code scripts/csv_to_seed.py} → 재기동(upsert 멱등). 자동 크롤링 없음.
 */
public interface ProductSource {

  List<SeedProduct> fetch();
}
