package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/**
 * 스타일 DNA·MCM 추천 포트 — 구현: Gemini HTTP(ai 서비스) 또는 mock(룰베이스).
 *
 * <p>추천 결과의 상품 id는 <b>신뢰 대상이 아니다</b>(계약 원칙) — 호출부가 DB 재검증 후 실재하는 것만 응답에 싣는다.
 */
public interface Recommender {

  StyleDnaResult styleDna(List<AiClosetItem> items);

  /** 후보 상품 중에서 추천 목록을 고른다. 첫 번째가 bestPick. */
  List<RecommendationPick> recommend(List<AiClosetItem> items, List<AiProduct> candidates);
}
