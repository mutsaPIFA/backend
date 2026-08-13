package com.mutsapifa.mcmmuse.auth.presentation.dto;

import com.mutsapifa.mcmmuse.auth.domain.User;

/** 계약 §1-5 — 200 {userId, email, nickname} */
public record MeResponse(Long userId, String email, String nickname) {

  public static MeResponse from(User user) {
    return new MeResponse(user.getId(), user.getEmail(), user.getNickname());
  }
}
