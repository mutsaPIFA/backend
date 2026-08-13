package com.mutsapifa.mcmmuse.styling.application.dto;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.List;

/** 계약 §4-4 응답 모양 — 프론트가 cutoutUrl들로 콜라주 렌더. */
public record OutfitResult(
    Long moodId,
    String occasionLabel,
    List<ItemSummary> closetItems,
    McmSummary mcmProduct,
    String reason) {

  public record ItemSummary(Long id, String cutoutUrl, Category category) {}

  public record McmSummary(Long id, String imageUrl, String cutoutUrl, String name) {}
}
