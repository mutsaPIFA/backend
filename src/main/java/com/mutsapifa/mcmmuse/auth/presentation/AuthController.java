package com.mutsapifa.mcmmuse.auth.presentation;

import com.mutsapifa.mcmmuse.auth.application.AuthService;
import com.mutsapifa.mcmmuse.auth.application.dto.AuthResult;
import com.mutsapifa.mcmmuse.auth.domain.exception.InvalidRefreshTokenException;
import com.mutsapifa.mcmmuse.auth.presentation.dto.LoginRequest;
import com.mutsapifa.mcmmuse.auth.presentation.dto.RegisterRequest;
import com.mutsapifa.mcmmuse.auth.presentation.dto.RegisterResponse;
import com.mutsapifa.mcmmuse.auth.presentation.dto.TokenResponse;
import com.mutsapifa.mcmmuse.shared.config.JwtProperties;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  static final String REFRESH_COOKIE = "refresh_token";

  /** refresh 쿠키는 auth 경로에만 전송된다 — 다른 API 호출에 딸려가지 않게. */
  private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

  private final AuthService authService;
  private final JwtProperties jwtProperties;

  public AuthController(AuthService authService, JwtProperties jwtProperties) {
    this.authService = authService;
    this.jwtProperties = jwtProperties;
  }

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      examples =
                          @io.swagger.v3.oas.annotations.media.ExampleObject(
                              name = "예시",
                              value =
                                  "{\"email\":\"demo@test.com\",\"password\":\"password123\","
                                      + "\"nickname\":\"데모\"}")))
          @Valid
          @RequestBody
          RegisterRequest request) {
    AuthResult result =
        authService.register(request.email(), request.password(), request.nickname());
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(RegisterResponse.of(result.userId(), result.accessToken()));
  }

  /** §1-6 — 게스트 발급: 입력 없이 계정 생성+로그인. QR 진입(웹 루트)에서 사용. */
  @PostMapping("/guest")
  public ResponseEntity<RegisterResponse> guest() {
    AuthResult result = authService.registerGuest();
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(RegisterResponse.of(result.userId(), result.accessToken()));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      examples =
                          @io.swagger.v3.oas.annotations.media.ExampleObject(
                              name = "스모크 계정",
                              value =
                                  "{\"email\":\"smoke0814@test.com\",\"password\":\"smoketest1!\"}")))
          @Valid
          @RequestBody
          LoginRequest request) {
    AuthResult result = authService.login(request.email(), request.password());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(TokenResponse.of(result.accessToken()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> refresh(
      @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new InvalidRefreshTokenException();
    }
    AuthResult result = authService.refresh(refreshToken);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie(result.refreshToken()).toString())
        .body(TokenResponse.of(result.accessToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(value = REFRESH_COOKIE, required = false) String refreshToken) {
    authService.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
        .build();
  }

  private ResponseCookie refreshCookie(String value) {
    return ResponseCookie.from(REFRESH_COOKIE, value)
        .httpOnly(true)
        .secure(jwtProperties.cookieSecure())
        .path(REFRESH_COOKIE_PATH)
        .sameSite("Lax")
        .maxAge(jwtProperties.refreshTokenTtl())
        .build();
  }

  private ResponseCookie expiredRefreshCookie() {
    return ResponseCookie.from(REFRESH_COOKIE, "")
        .httpOnly(true)
        .secure(jwtProperties.cookieSecure())
        .path(REFRESH_COOKIE_PATH)
        .sameSite("Lax")
        .maxAge(Duration.ZERO)
        .build();
  }
}
