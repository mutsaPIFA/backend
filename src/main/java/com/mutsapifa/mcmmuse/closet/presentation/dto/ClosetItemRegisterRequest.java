package com.mutsapifa.mcmmuse.closet.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import com.mutsapifa.mcmmuse.shared.vocab.Source;

/**
 * POST /closet-items — 계약상 한 경로에 두 모양의 body가 온다:
 *
 * <ul>
 *   <li>§3-2 스캔 결과 등록: source + 태그 4종 + imageUrl (+cutoutUrl)
 *   <li>§3-3 카탈로그 담기: mcmProductId 하나만
 * </ul>
 *
 * {@code mcmProductId} 유무로 분기하므로 필드는 전부 optional로 받고 컨트롤러에서 검증한다.
 *
 * <p>{@code name}(§3-2 확장): 스캔 직후 사용자가 정한 명칭 — 생략 시 태그 조합 표시(§3-6과 같은 의미론).
 */
public record ClosetItemRegisterRequest(
    String name,
    Source source,
    Category category,
    Color color,
    Material material,
    ItemMood mood,
    String imageUrl,
    String cutoutUrl,
    Long mcmProductId) {

  // 파생 헬퍼 — Jackson 프로퍼티로 인식돼 Swagger 예시 body에 "catalogAdd"로 새는 것 차단
  @JsonIgnore
  public boolean isCatalogAdd() {
    return mcmProductId != null;
  }

  public boolean hasScanFields() {
    return source != null
        && category != null
        && color != null
        && material != null
        && mood != null
        && imageUrl != null
        && !imageUrl.isBlank();
  }
}
