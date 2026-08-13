package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/** 비전 태깅 결과 — controlled vocabulary로 정규화된 값만 담는다. */
public record ScanTags(Category category, Color color, Material material, ItemMood mood) {}
