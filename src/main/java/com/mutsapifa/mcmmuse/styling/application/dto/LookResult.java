package com.mutsapifa.mcmmuse.styling.application.dto;

import com.mutsapifa.mcmmuse.styling.domain.Look;
import java.time.LocalDate;
import java.util.List;

/** 계약 §4-5 응답 모양 — Look. */
public record LookResult(
    Long id,
    Long userId,
    LocalDate wornDate,
    Long moodId,
    String occasionLabel,
    String concept,
    List<Long> closetItemIds,
    Long mcmProductId,
    String note,
    String reason,
    String generatedImageUrl) {

  public static LookResult from(Look look, String occasionLabel) {
    return new LookResult(
        look.getId(),
        look.getUserId(),
        look.getWornDate(),
        look.getMoodId(),
        occasionLabel,
        look.getConcept(),
        // lazy 컬렉션을 트랜잭션 안에서 강제 초기화·복사 — 참조를 그대로 들고 나가면
        // Jackson 직렬화 시점(tx 밖)에 LazyInitializationException
        List.copyOf(look.getClosetItemIds()),
        look.getMcmProductId(),
        look.getNote(),
        look.getReason(),
        look.getGeneratedImageUrl());
  }
}
