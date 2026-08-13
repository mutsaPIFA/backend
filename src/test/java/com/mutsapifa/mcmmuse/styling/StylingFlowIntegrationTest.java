package com.mutsapifa.mcmmuse.styling;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** styling(DNA·추천) 플로우 — 계약 §4-1·§4-2. 룰베이스 mock Recommender 사용. */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"app.storage.local-path=build/test-uploads"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StylingFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;
  @Autowired McmProductRepository mcmProductRepository;

  static String token;
  static String otherToken;
  static Long itemNavyTop;
  static Long itemNavyPants;

  private HttpHeaders auth(String t) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(t);
    return h;
  }

  private String register(String email) {
    return (String)
        rest.postForEntity(
                "/api/v1/auth/register",
                Map.of("email", email, "password", "password123", "nickname", "styler"),
                Map.class)
            .getBody()
            .get("accessToken");
  }

  private Long addItem(String token, String category, String color, String material, String mood) {
    return ((Number)
            rest.exchange(
                    "/api/v1/closet-items",
                    HttpMethod.POST,
                    new HttpEntity<>(
                        Map.of(
                            "source",
                            "OWN",
                            "category",
                            category,
                            "color",
                            color,
                            "material",
                            material,
                            "mood",
                            mood,
                            "imageUrl",
                            "http://x/i.jpg",
                            "cutoutUrl",
                            "http://x/i-cut.png"),
                        auth(token)),
                    Map.class)
                .getBody()
                .get("id"))
        .longValue();
  }

  @Test
  @Order(1)
  void 준비_유저_옷장_상품() {
    token = register("style-a@test.com");
    otherToken = register("style-b@test.com");
    itemNavyTop = addItem(token, "상의", "네이비", "면", "클래식");
    itemNavyPants = addItem(token, "하의", "네이비", "데님", "미니멀");

    mcmProductRepository.save(
        new McmProduct(
            "STYSKU001",
            "Tracy 비세토스 숄더백",
            Category.가방,
            Color.카멜,
            Material.가죽,
            1450000,
            "https://img/1.jpg",
            "https://mcm/1"));
    mcmProductRepository.save(
        new McmProduct(
            "STYSKU002",
            "네이비 모노그램 셔츠",
            Category.상의,
            Color.네이비,
            Material.면,
            830000,
            "https://img/2.jpg",
            "https://mcm/2"));
    mcmProductRepository.save(
        new McmProduct(
            "STYSKU003",
            "핑크 비세토스 카드홀더",
            Category.악세서리,
            Color.핑크,
            Material.가죽,
            350000,
            "https://img/3.jpg",
            "https://mcm/3"));
    assertThat(itemNavyTop).isNotNull();
  }

  @Test
  @Order(2)
  void 스타일DNA_200_지배색_무드() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/style-dna",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("closetItemIds", List.of(itemNavyTop, itemNavyPants)), auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat((List<String>) res.getBody().get("dominantColors")).contains("네이비");
    assertThat((String) res.getBody().get("summary")).isNotBlank();
    assertThat((List<String>) res.getBody().get("keywords")).isNotEmpty();
  }

  @Test
  @Order(3)
  void 추천_200_bestPick_재검증된_상품() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/recommendations",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("closetItemIds", List.of(itemNavyTop, itemNavyPants)), auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    Map bestPick = (Map) res.getBody().get("bestPick");
    assertThat(bestPick).isNotNull();
    Map product = (Map) bestPick.get("product");
    assertThat(product).containsKeys("id", "name", "imageUrl", "price", "productUrl");
    assertThat((Boolean) bestPick.get("isExpansion")).isFalse();
    assertThat((String) bestPick.get("reason")).isNotBlank();
    assertThat((List<?>) bestPick.get("pairsWithItemIds")).isNotEmpty();
    assertThat((List<?>) res.getBody().get("more")).isNotEmpty();
  }

  @Test
  @Order(4)
  void 남의_아이템으로_요청_403() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/style-dna",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("closetItemIds", List.of(itemNavyTop)), auth(otherToken)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(403);
  }

  @Test
  @Order(5)
  void 없는_아이템만으로_요청_400() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/recommendations",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("closetItemIds", List.of(999999L)), auth(token)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  @Order(6)
  void 빈_배열_400() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/style-dna",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("closetItemIds", List.of()), auth(token)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(400);
  }
}
