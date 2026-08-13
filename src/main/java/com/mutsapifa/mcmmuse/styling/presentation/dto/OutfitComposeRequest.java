package com.mutsapifa.mcmmuse.styling.presentation.dto;

import jakarta.validation.constraints.NotNull;

/** 계약 §4-4 — {moodId, seedMcmProductId?} */
public record OutfitComposeRequest(
    @NotNull(message = "무드를 선택해 주세요") Long moodId, Long seedMcmProductId) {}
