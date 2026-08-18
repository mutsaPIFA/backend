package com.mutsapifa.mcmmuse.styling.application.dto;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.util.List;

/**
 * 계약 §4-4 응답 모양 — {@code imageUrl} = AI 화보(연출컷). 생성에 실패한 후보는 응답에서 제외된다(OutfitImageService).
 */
public record OutfitResult(
    Long moodId,
    String occasionLabel,
    String concept,
    String imageUrl,
    List<ItemSummary> closetItems,
    McmSummary mcmProduct,
    String reason) {

  public OutfitResult withImageUrl(String url) {
    return new OutfitResult(moodId, occasionLabel, concept, url, closetItems, mcmProduct, reason);
  }

  /** color·material은 표시용(프론트가 "블랙 가죽 가방"식 이름 조합) — 계약 §4-4 */
  public record ItemSummary(Long id, String cutoutUrl, Category category, Color color, Material material) {}

  public record McmSummary(Long id, String imageUrl, String cutoutUrl, String name) {}
}
