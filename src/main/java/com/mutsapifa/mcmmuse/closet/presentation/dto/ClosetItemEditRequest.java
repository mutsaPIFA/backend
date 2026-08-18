package com.mutsapifa.mcmmuse.closet.presentation.dto;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import jakarta.validation.constraints.Size;

/** 계약 §3-6 — 모든 필드 선택(부분 수정). name 빈 문자열 = 명칭 제거. */
public record ClosetItemEditRequest(
    @Size(max = 30, message = "명칭은 30자 이하여야 합니다") String name,
    Category category,
    Color color,
    Material material,
    ItemMood mood) {}
