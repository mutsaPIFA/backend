package com.mutsapifa.mcmmuse.auth.presentation.dto;

import com.mutsapifa.mcmmuse.auth.domain.User;
import com.mutsapifa.mcmmuse.profile.domain.UserStyleDna;
import java.time.Instant;
import java.util.List;

/** 계약 §1-5 — styleDna는 최근 분석 스냅샷(§4-1 부수효과), 없으면 null. */
public record MeResponse(
    Long userId, String email, String nickname, String avatarUrl, StyleDna styleDna) {

  public record StyleDna(
      String summary,
      List<String> dominantColors,
      List<String> dominantMoods,
      List<String> keywords,
      Instant updatedAt) {}

  public static MeResponse of(User user, UserStyleDna dna) {
    return new MeResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getAvatarUrl(),
        dna == null
            ? null
            : new StyleDna(
                dna.getSummary(),
                dna.getDominantColors(),
                dna.getDominantMoods(),
                dna.getKeywords(),
                dna.getUpdatedAt()));
  }
}
