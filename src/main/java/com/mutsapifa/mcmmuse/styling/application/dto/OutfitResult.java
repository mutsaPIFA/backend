package com.mutsapifa.mcmmuse.styling.application.dto;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.List;

/**
 * 계약 §4-4 응답 모양 — {@code imageUrl} = AI 화보(연출컷). 생성 실패 시 null이면 프론트가 cutoutUrl들로 콜라주 폴백 렌더.
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

  public record ItemSummary(Long id, String cutoutUrl, Category category) {}

  public record McmSummary(Long id, String imageUrl, String cutoutUrl, String name) {}
}
