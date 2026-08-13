package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/**
 * 코디 조합 포트 — 구현: Gemini HTTP(ai 서비스 /outfits) 또는 mock(룰베이스).
 *
 * <p>각 코디는 MCM 제품을 정확히 1개 포함한다(계약 D5). 후보는 최대 3개, 재료 부족 시 1~2개.
 */
public interface OutfitComposer {

  /**
   * @param moodLabel 무드 라벨 (reason 문구용, 예: "저녁 약속")
   * @param ownItems 옷장 OWN 아이템
   * @param mcmCandidates 코디에 넣을 수 있는 MCM 제품 (seed 고정 시 1개짜리 목록)
   */
  List<OutfitPick> compose(
      String moodLabel, List<AiClosetItem> ownItems, List<AiProduct> mcmCandidates);
}
