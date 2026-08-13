package com.mutsapifa.mcmmuse.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 에러 응답 (계약 api-v1.md "공통 응답 / 에러").
 *
 * <p>{@code code}는 프론트가 분기해야 하는 비즈니스 에러에만 부여한다 — 그 외엔 null이라 응답에서 빠진다.
 */
public record ApiError(
    int status, String message, @JsonInclude(JsonInclude.Include.NON_NULL) String code) {

  public static ApiError of(int status, String message) {
    return new ApiError(status, message, null);
  }
}
