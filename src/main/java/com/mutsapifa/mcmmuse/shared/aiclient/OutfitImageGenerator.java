package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/**
 * 코디 화보 생성 포트 — 아이템 누끼 이미지들 → flat-lay 연출컷 1장 (계약 §4-4 imageUrl).
 *
 * <p>mock은 null을 반환한다(후보 imageUrl=null → 프론트 누끼 콜라주 폴백). 저장(§4-5)은 후보 화보를 재사용하므로 이 포트를 다시 타지
 * 않는다.
 */
public interface OutfitImageGenerator {

  /** 생성 실패·mock이면 null — 호출부는 null을 폴백 신호로 다룬다. */
  byte[] generate(List<byte[]> images);
}
