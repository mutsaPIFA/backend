package com.mutsapifa.mcmmuse.catalog.presentation.dto;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/** 계약 §2-1 — {id, name, category, color, material, price, imageUrl, cutoutUrl, productUrl} */
public record McmProductResponse(
    Long id,
    String name,
    Category category,
    Color color,
    Material material,
    Integer price,
    String imageUrl,
    String cutoutUrl,
    String productUrl) {

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
        p.getProductUrl());
  }
}
