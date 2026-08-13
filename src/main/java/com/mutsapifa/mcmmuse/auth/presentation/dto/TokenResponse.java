package com.mutsapifa.mcmmuse.auth.presentation.dto;

/** 계약 §1-2 / §1-3 — 200 {accessToken, tokenType} */
public record TokenResponse(String accessToken, String tokenType) {

  public static TokenResponse of(String accessToken) {
    return new TokenResponse(accessToken, "Bearer");
  }
}
