package com.mutsapifa.mcmmuse.styling.presentation.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 계약 §4-1·§4-2 공통 요청 — {closetItemIds: [1, 5, 12]} */
public record ClosetItemIdsRequest(
    @NotEmpty(message = "옷장 아이템을 1개 이상 선택해 주세요") List<Long> closetItemIds) {}
