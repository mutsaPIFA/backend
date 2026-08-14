package com.mutsapifa.mcmmuse.catalog.presentation.dto;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.util.List;

/** 계약 §2-1 — 상세 화면(6/7-a)용 description·size·imageUrls(캐러셀) 포함. */
public record McmProductResponse(
    Long id,
    String name,
    Category category,
    Color color,
    Material material,
    Integer price,
    String imageUrl,
    String cutoutUrl,
    String productUrl,
    String description,
    String size,
    List<String> imageUrls) {

  public static McmProductResponse from(McmProduct p) {
    return new McmProductResponse(
        p.getId(),
        p.getName(),
        p.getCategory(),
        p.getColor(),
        p.getMaterial(),
        p.getPrice(),
        p.getImageUrl(),
        p.getCutoutUrl(),
        p.getProductUrl(),
        p.getDescription(),
        p.getSize(),
        // 컨버터가 불변 리스트를 주지만 복사해 직렬화 안전 확보 (lazy 아님 — 관례 유지)
        List.copyOf(p.getImageUrls()));
  }
}
