package com.mutsapifa.mcmmuse.catalog.application;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/** 적재 원천 데이터 1건 — vocabulary enum 파싱을 통과해야 적재된다 (DB-컨벤션). */
public record SeedProduct(
    String sku,
    String name,
    Category category,
    Color color,
    Material material,
    Integer price,
    String imageUrl,
    String productUrl) {}
