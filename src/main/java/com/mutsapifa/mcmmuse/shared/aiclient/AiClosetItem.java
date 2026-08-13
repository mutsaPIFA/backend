package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/** AI 포트 입력용 옷장 아이템 요약 — shared는 BC 엔티티를 모르므로 값만 넘긴다. */
public record AiClosetItem(
    Long id, Category category, Color color, Material material, ItemMood mood) {}
