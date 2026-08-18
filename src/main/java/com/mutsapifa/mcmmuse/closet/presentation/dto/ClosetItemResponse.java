package com.mutsapifa.mcmmuse.closet.presentation.dto;

import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import java.time.Instant;

/** 계약 §3-2 응답 — ClosetItem. {@code name}=사용자 지정 명칭(§3-6), null이면 프론트가 태그 조합으로 표시. */
public record ClosetItemResponse(
    Long id,
    Long userId,
    String name,
    Category category,
    Color color,
    Material material,
    ItemMood mood,
    String imageUrl,
    String cutoutUrl,
    Source source,
    Long mcmProductId,
    Instant createdAt) {

  public static ClosetItemResponse from(ClosetItem item) {
    return new ClosetItemResponse(
        item.getId(),
        item.getUserId(),
        item.getCustomName(),
        item.getCategory(),
        item.getColor(),
        item.getMaterial(),
        item.getMood(),
        item.getImageUrl(),
        item.getCutoutUrl(),
        item.getSource(),
        item.getMcmProductId(),
        item.getCreatedAt());
  }
}
