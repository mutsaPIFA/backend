package com.mutsapifa.mcmmuse.auth.presentation.dto;

/** 계약 §1-1 — 201 {userId, accessToken, tokenType} */
public record RegisterResponse(Long userId, String accessToken, String tokenType) {

  public static RegisterResponse of(Long userId, String accessToken) {
    return new RegisterResponse(userId, accessToken, "Bearer");
  }
}
