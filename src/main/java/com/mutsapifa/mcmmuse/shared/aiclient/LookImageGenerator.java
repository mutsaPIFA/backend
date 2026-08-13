package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/**
 * 룩 이미지 생성 포트 (계약 D2 — 저장한 룩 1개에만, 비동기 호출).
 *
 * <p>구현: Gemini flat-lay HTTP(ai 서비스 /looks/image) 또는 mock(생성 안 함 — null). mock이 가짜 이미지를 채우지 않는 이유:
 * 계약이 {@code generatedImageUrl=null}을 허용하고 프론트가 플레이스홀더를 처리하므로, 정직하게 비워둔다.
 */
public interface LookImageGenerator {

  /**
   * @param itemCutoutUrls 옷장 아이템 누끼 URL들
   * @param mcmImageUrl MCM 제품 이미지 URL
   * @return 생성된 이미지 바이트, 생성 불가(mock·실패)면 null
   */
  byte[] generate(List<String> itemCutoutUrls, String mcmImageUrl);
}
