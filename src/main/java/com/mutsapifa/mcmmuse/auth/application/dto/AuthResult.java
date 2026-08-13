package com.mutsapifa.mcmmuse.auth.application.dto;

/** 서비스 → 컨트롤러 내부 DTO. refreshToken은 쿠키로만 나가고 body에는 싣지 않는다. */
public record AuthResult(Long userId, String accessToken, String refreshToken) {}
