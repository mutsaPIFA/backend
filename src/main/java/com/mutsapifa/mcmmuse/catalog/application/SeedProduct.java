package com.mutsapifa.mcmmuse.catalog.application;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.util.List;

/** 적재 원천 데이터 1건 — vocabulary enum 파싱을 통과해야 적재된다 (DB-컨벤션). */
public record SeedProduct(
    String sku,
    String name,
    Category category,
    Color color,
    Material material,
    Integer price,
    String imageUrl,
    String productUrl,
    String description,
    String size,
    List<String> imageUrls,
    /** scripts/tag_product_mood.py 태깅 결과 — 태깅 전 시드면 null */
    ItemMood mood,
    /** 스타일 한 줄 요약(shortDesc) — 추천 프롬프트 입력 */
    String styleNote) {}
