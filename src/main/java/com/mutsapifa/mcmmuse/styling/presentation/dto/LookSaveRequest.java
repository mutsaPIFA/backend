package com.mutsapifa.mcmmuse.styling.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** 계약 §4-5 — wornDate는 옵션(미전송 시 서버가 오늘 날짜, 결정 D9). */
public record LookSaveRequest(
    @NotNull(message = "무드를 선택해 주세요") Long moodId,
    @NotEmpty(message = "옷장 아이템을 1개 이상 선택해 주세요") List<Long> closetItemIds,
    @NotNull(message = "코디에 포함된 MCM 제품이 필요합니다") Long mcmProductId,
    String imageUrl,
    String reason,
    LocalDate wornDate) {}
