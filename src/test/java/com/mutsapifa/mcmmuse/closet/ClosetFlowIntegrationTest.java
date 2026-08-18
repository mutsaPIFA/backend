package com.mutsapifa.mcmmuse.closet;

import static org.assertj.core.api.Assertions.assertThat;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** closet 전체 플로우 — 계약 §3 (스캔은 mock AI 포트, 저장은 build/test-uploads). */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.storage.local-path=build/test-uploads",
      "app.ai.cutout=mock",
      "app.ai.tagging=mock",
      "app.ai.standardize=mock",
      "app.ai.outfit-image=mock",
      "app.ai.outfit-compose=mock",
      "app.ai.recommend=mock"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClosetFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;
  @Autowired McmProductRepository mcmProductRepository;

  static String token;
  static String otherToken;
  static Map scanBody;
  static Long itemId;
  static Long productId;

  private HttpHeaders auth(String t) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(t);
    return headers;
  }

  @Test
  @Order(1)
  void 준비_유저2명_가입() {
    token = register("closet-a@test.com");
    otherToken = register("closet-b@test.com");
    assertThat(token).isNotBlank();
  }

  private String register(String email) {
    ResponseEntity<Map> res =
        rest.postForEntity(
            "/api/v1/auth/register",
            Map.of("email", email, "password", "password123", "nickname", "옷장러"),
            Map.class);
    return (String) res.getBody().get("accessToken");
  }

  @Test
  @Order(2)
  void 스캔_200_url과_vocab태그() {
    HttpHeaders headers = auth(token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.IMAGE_JPEG);
    parts.add(
        "image",
        new HttpEntity<>(
            new ByteArrayResource("fake-jpeg-bytes".getBytes()) {
              @Override
              public String getFilename() {
                return "shirt.jpg";
              }
            },
            partHeaders));

    ResponseEntity<Map> res =
        rest.exchange("/api/v1/scan", HttpMethod.POST, new HttpEntity<>(parts, headers), Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    scanBody = res.getBody();
    assertThat((String) scanBody.get("originalUrl")).contains("/images/scan/");
    assertThat((String) scanBody.get("cutoutUrl")).contains("/images/scan/");
    Map tags = (Map) scanBody.get("tags");
    assertThat(Category.valueOf((String) tags.get("category"))).isNotNull(); // vocab 강제 확인
  }

  @Test
  @Order(3)
  void 스캔이미지_정적서빙_200() {
    String path = ((String) scanBody.get("originalUrl")).replace("http://localhost:8080", "");
    ResponseEntity<byte[]> res =
        rest.exchange(
            "/api/v1/../..".replace("/api/v1/../..", path),
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            byte[].class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).isNotEmpty();
  }

  @Test
  @Order(4)
  void 등록_태그수정해서_201() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/closet-items",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "source", "OWN",
                    "category", "상의",
                    "color", "네이비", // AI 태그를 사용자가 수정했다고 가정
                    "material", "면",
                    "mood", "클래식",
                    "imageUrl", scanBody.get("originalUrl"),
                    "cutoutUrl", scanBody.get("cutoutUrl")),
                auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(201);
    assertThat(res.getBody().get("color")).isEqualTo("네이비");
    assertThat(res.getBody().get("source")).isEqualTo("OWN");
    itemId = ((Number) res.getBody().get("id")).longValue();
  }

  @Test
  @Order(5)
  void vocabulary_밖_값은_400() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/closet-items",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "source",
                    "OWN",
                    "category",
                    "상의",
                    "color",
                    "빨강", // vocab 밖
                    "material",
                    "면",
                    "mood",
                    "클래식",
                    "imageUrl",
                    "http://x/1.jpg"),
                auth(token)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  @Order(6)
  void 카탈로그_담기_201_source_MCM() {
    productId =
        mcmProductRepository
            .save(
                new McmProduct(
                    "TESTSKU001",
                    "Tracy 숄더백",
                    Category.가방,
                    Color.카멜,
                    Material.가죽,
                    890000,
                    "https://img/1.jpg",
                    "https://mcm/p/1",
                    null,
                    null,
                    List.of(),
                    null,
                    null))
            .getId();

    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/closet-items",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("mcmProductId", productId), auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(201);
    assertThat(res.getBody().get("source")).isEqualTo("MCM");
    assertThat(((Number) res.getBody().get("mcmProductId")).longValue()).isEqualTo(productId);
    assertThat(res.getBody().get("category")).isEqualTo("가방");
  }

  @Test
  @Order(7)
  void 없는_제품_담기_404() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/closet-items",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("mcmProductId", 999999), auth(token)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @Order(8)
  void 목록_최신순_source필터() {
    ResponseEntity<List> all =
        rest.exchange(
            "/api/v1/closet-items", HttpMethod.GET, new HttpEntity<>(auth(token)), List.class);
    assertThat(all.getBody()).hasSize(2);
    // createdAt DESC — 나중에 담은 MCM이 먼저
    assertThat(((Map) all.getBody().get(0)).get("source")).isEqualTo("MCM");

    ResponseEntity<List> own =
        rest.exchange(
            "/api/v1/closet-items?source=OWN",
            HttpMethod.GET,
            new HttpEntity<>(auth(token)),
            List.class);
    assertThat(own.getBody()).hasSize(1);
  }

  @Test
  @Order(9)
  void 남의_아이템_삭제_403() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/closet-items/" + itemId,
            HttpMethod.DELETE,
            new HttpEntity<>(auth(otherToken)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(403);
  }

  @Test
  @Order(10)
  void 소프트삭제_204_목록에서_사라짐_재삭제_404() {
    ResponseEntity<Void> del =
        rest.exchange(
            "/api/v1/closet-items/" + itemId,
            HttpMethod.DELETE,
            new HttpEntity<>(auth(token)),
            Void.class);
    assertThat(del.getStatusCode().value()).isEqualTo(204);

    ResponseEntity<List> all =
        rest.exchange(
            "/api/v1/closet-items", HttpMethod.GET, new HttpEntity<>(auth(token)), List.class);
    assertThat(all.getBody()).hasSize(1); // MCM만 남음

    ResponseEntity<Map> again =
        rest.exchange(
            "/api/v1/closet-items/" + itemId,
            HttpMethod.DELETE,
            new HttpEntity<>(auth(token)),
            Map.class);
    assertThat(again.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @Order(11)
  void 토큰없으면_401() {
    ResponseEntity<Map> res = rest.getForEntity("/api/v1/closet-items", Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(401);
  }
}
