package com.mutsapifa.mcmmuse.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** auth 전체 플로우 — 실제 PostgreSQL(Testcontainers) 위에서 계약(api-v1.md §1) 검증. */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.ai.cutout=mock",
      "app.ai.tagging=mock",
      "app.ai.standardize=mock",
      "app.ai.outfit-image=mock",
      "app.ai.outfit-compose=mock",
      "app.ai.recommend=mock"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;

  static String accessToken;
  static String refreshCookie;

  @Test
  @Order(1)
  void 회원가입_201_토큰과_refresh쿠키() {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", "Muse@Example.com", "password", "password123", "nickname", "뮤즈"),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(201);
    assertThat(res.getBody()).containsKeys("userId", "accessToken");
    assertThat(res.getBody().get("tokenType")).isEqualTo("Bearer");
    String setCookie = res.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookie).contains("refresh_token=").contains("HttpOnly");

    accessToken = (String) res.getBody().get("accessToken");
    refreshCookie = setCookie.split(";")[0];
  }

  @Test
  @Order(2)
  void 이메일_중복_409() {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", "muse@example.com", "password", "password123", "nickname", "뮤즈2"),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(409);
    assertThat(res.getBody().get("message")).isEqualTo("이미 가입된 이메일입니다");
  }

  @Test
  @Order(3)
  void 검증실패_400() {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", "not-an-email", "password", "short", "nickname", ""),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(400);
    assertThat(res.getBody().get("status")).isEqualTo(400);
  }

  @Test
  @Order(4)
  void 로그인_대소문자_무관_200() {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", "MUSE@example.com", "password", "password123"),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).containsKey("accessToken");
  }

  @Test
  @Order(5)
  void 비밀번호_불일치_401() {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", "muse@example.com", "password", "wrong-password"),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(401);
  }

  @Test
  @Order(6)
  void me_토큰으로_200_토큰없으면_401() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<Map> res =
        rest.exchange("/api/v1/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody().get("email")).isEqualTo("muse@example.com");
    assertThat(res.getBody().get("nickname")).isEqualTo("뮤즈");

    ResponseEntity<Map> noAuth = rest.getForEntity("/api/v1/me", Map.class);
    assertThat(noAuth.getStatusCode().value()).isEqualTo(401);
  }

  @Test
  @Order(7)
  void refresh_회전_이전토큰은_재사용_불가() {
    // 1차 갱신 성공 + 새 쿠키
    ResponseEntity<Map> first = postWithCookie("/api/v1/auth/refresh", refreshCookie);
    assertThat(first.getStatusCode().value()).isEqualTo(200);
    assertThat(first.getBody()).containsKey("accessToken");
    String rotated = first.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    assertThat(rotated).isNotEqualTo(refreshCookie);

    // 소모된 이전 쿠키로 재시도 → 401 (회전 검증)
    ResponseEntity<Map> replay = postWithCookie("/api/v1/auth/refresh", refreshCookie);
    assertThat(replay.getStatusCode().value()).isEqualTo(401);

    refreshCookie = rotated;
  }

  @Test
  @Order(8)
  void 로그아웃_204_이후_refresh_401() {
    ResponseEntity<Map> logout = postWithCookie("/api/v1/auth/logout", refreshCookie);
    assertThat(logout.getStatusCode().value()).isEqualTo(204);

    ResponseEntity<Map> after = postWithCookie("/api/v1/auth/refresh", refreshCookie);
    assertThat(after.getStatusCode().value()).isEqualTo(401);
  }

  private ResponseEntity<Map> postWithCookie(String path, String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.put(HttpHeaders.COOKIE, List.of(cookie));
    return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
  }
}
