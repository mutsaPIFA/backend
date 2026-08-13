package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/** AI 포트 입력용 MCM 상품 후보 요약. */
public record AiProduct(Long id, String name, Category category, Color color, Material material) {}
