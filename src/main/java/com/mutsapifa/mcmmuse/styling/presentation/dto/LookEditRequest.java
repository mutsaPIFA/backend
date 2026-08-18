package com.mutsapifa.mcmmuse.styling.presentation.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 계약 §4-9 — 모든 필드 선택(부분 수정). note 빈 문자열 = 소감 제거. */
public record LookEditRequest(
    @Size(max = 1000, message = "소감은 1,000자 이하여야 합니다") String note, LocalDate wornDate) {}
