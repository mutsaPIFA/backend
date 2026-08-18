package com.mutsapifa.mcmmuse.styling;

import static org.assertj.core.api.Assertions.assertThat;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.time.LocalDate;
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

/** 큐레이터 플로우 — 계약 §4-3~4-7 (무드→코디 후보→룩 저장→폴링→기록). */
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
class CurationFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;
  @Autowired McmProductRepository mcmProductRepository;

  static String token;
  static String otherToken;
  static Long productId;
  static Long lookId;
  static List<Long> outfitItemIds;
  static Long outfitMcmId;

  private HttpHeaders auth(String t) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(t);
    return h;
  }

  private Long addItem(String category, String color, String material, String mood) {
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
  void 무드_시드_6개() {
    token =
        (String)
            rest.postForEntity(
                    "/api/v1/auth/register",
                    Map.of("email", "cur-a@test.com", "password", "password123", "nickname", "cur"),
                    Map.class)
                .getBody()
                .get("accessToken");
    otherToken =
        (String)
            rest.postForEntity(
                    "/api/v1/auth/register",
                    Map.of(
                        "email", "cur-b@test.com", "password", "password123", "nickname", "cur2"),
                    Map.class)
                .getBody()
                .get("accessToken");

    ResponseEntity<List> res =
        rest.exchange("/api/v1/moods", HttpMethod.GET, new HttpEntity<>(auth(token)), List.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).hasSize(6);
    Map first = (Map) res.getBody().get(0);
    assertThat(first).containsKeys("id", "label", "labelEn", "iconKey");
  }

  @Test
  @Order(2)
  void 옷장에_MCM_없으면_409_코드() {
    addItem("상의", "네이비", "면", "클래식");
    addItem("하의", "네이비", "데님", "미니멀");

    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/outfits",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("moodId", 1), auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(409);
    assertThat(res.getBody().get("code")).isEqualTo("NO_MCM_IN_CLOSET");
  }

  @Test
  @Order(3)
  void MCM_담은_후_코디후보_생성() {
    productId =
        mcmProductRepository
            .save(
                new McmProduct(
                    "CURSKU001",
                    "Tracy 비세토스 숄더백",
                    Category.가방,
                    Color.카멜,
                    Material.가죽,
                    1450000,
                    "https://img/1.jpg",
                    "https://mcm/1",
                    null,
                    null,
                    List.of(),
                    null,
                    null))
            .getId();
    // 카탈로그 담기 → 옷장에 source=MCM 아이템 생성
    rest.exchange(
        "/api/v1/closet-items",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("mcmProductId", productId), auth(token)),
        Map.class);

    ResponseEntity<List> res =
        rest.exchange(
            "/api/v1/outfits",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("moodId", 1), auth(token)),
            List.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).isNotEmpty().hasSizeLessThanOrEqualTo(3);
    Map outfit = (Map) res.getBody().get(0);
    assertThat(outfit.get("occasionLabel")).isEqualTo("저녁 약속 / DINNER DATE");
    Map mcm = (Map) outfit.get("mcmProduct");
    assertThat(((Number) mcm.get("id")).longValue()).isEqualTo(productId);
    List<Map> closetItems = (List<Map>) outfit.get("closetItems");
    assertThat(closetItems).isNotEmpty();
    assertThat(closetItems.get(0)).containsKeys("id", "cutoutUrl", "category");
    assertThat((String) outfit.get("reason")).isNotBlank();

    outfitItemIds = closetItems.stream().map(m -> ((Number) m.get("id")).longValue()).toList();
    outfitMcmId = ((Number) mcm.get("id")).longValue();
  }

  @Test
  @Order(4)
  void seed_고정_큐레이팅() {
    ResponseEntity<List> res =
        rest.exchange(
            "/api/v1/outfits",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("moodId", 2, "seedMcmProductId", productId), auth(token)),
            List.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    Map outfit = (Map) res.getBody().get(0);
    assertThat(((Number) ((Map) outfit.get("mcmProduct")).get("id")).longValue())
        .isEqualTo(productId);
  }

  @Test
  @Order(5)
  void 없는_무드_404() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/outfits",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("moodId", 999), auth(token)),
            Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @Order(6)
  void 룩저장_201_wornDate_기본오늘_이미지null() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/looks",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "moodId",
                    1,
                    "closetItemIds",
                    outfitItemIds,
                    "mcmProductId",
                    outfitMcmId,
                    "reason",
                    "클래식 조합 + 비세토스 포인트"),
                auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(201);
    assertThat(res.getBody().get("wornDate")).isEqualTo(LocalDate.now().toString());
    assertThat(res.getBody().get("generatedImageUrl")).isNull(); // imageUrl 미전달 — null 유지
    assertThat(res.getBody().get("occasionLabel")).isEqualTo("저녁 약속 / DINNER DATE");
    lookId = ((Number) res.getBody().get("id")).longValue();
  }

  @Test
  @Order(20)
  void 룩저장_후보_화보_imageUrl_컨셉_소감_수납() {
    String imageUrl = "http://localhost:8080/images/outfits/test-board.png";
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/looks",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "moodId",
                    1,
                    "closetItemIds",
                    outfitItemIds,
                    "mcmProductId",
                    outfitMcmId,
                    "imageUrl",
                    imageUrl,
                    "concept",
                    "Soft Classic",
                    "note",
                    "오늘 옷 센스 있다는 말 들은 날"),
                auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(201);
    assertThat(res.getBody().get("generatedImageUrl")).isEqualTo(imageUrl); // 저장 즉시 확정 — 폴링 불필요
    assertThat(res.getBody().get("concept")).isEqualTo("Soft Classic");
    assertThat(res.getBody().get("note")).isEqualTo("오늘 옷 센스 있다는 말 들은 날");
  }

  @Test
  @Order(21)
  void 룩저장_외부_imageUrl_400() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/looks",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "moodId",
                    1,
                    "closetItemIds",
                    outfitItemIds,
                    "mcmProductId",
                    outfitMcmId,
                    "imageUrl",
                    "https://malicious.example.com/x.png"),
                auth(token)),
            Map.class);

    assertThat(res.getStatusCode().value()).isEqualTo(400); // 우리 스토리지 URL만 수납
  }

  @Test
  @Order(7)
  void 룩_단건_폴링_200_남의것_403_없는것_404() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/looks/" + lookId, HttpMethod.GET, new HttpEntity<>(auth(token)), Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(((Number) res.getBody().get("id")).longValue()).isEqualTo(lookId);

    ResponseEntity<Map> other =
        rest.exchange(
            "/api/v1/looks/" + lookId,
            HttpMethod.GET,
            new HttpEntity<>(auth(otherToken)),
            Map.class);
    assertThat(other.getStatusCode().value()).isEqualTo(403);

    ResponseEntity<Map> missing =
        rest.exchange(
            "/api/v1/looks/999999", HttpMethod.GET, new HttpEntity<>(auth(token)), Map.class);
    assertThat(missing.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @Order(8)
  void 룩_목록_month_필터() {
    String thisMonth = LocalDate.now().toString().substring(0, 7);

    ResponseEntity<List> all =
        rest.exchange("/api/v1/looks", HttpMethod.GET, new HttpEntity<>(auth(token)), List.class);
    assertThat(all.getBody()).hasSize(1);

    ResponseEntity<List> filtered =
        rest.exchange(
            "/api/v1/looks?month=" + thisMonth,
            HttpMethod.GET,
            new HttpEntity<>(auth(token)),
            List.class);
    assertThat(filtered.getBody()).hasSize(1);

    ResponseEntity<List> empty =
        rest.exchange(
            "/api/v1/looks?month=2020-01",
            HttpMethod.GET,
            new HttpEntity<>(auth(token)),
            List.class);
    assertThat(empty.getBody()).isEmpty();

    ResponseEntity<Map> bad =
        rest.exchange(
            "/api/v1/looks?month=notamonth",
            HttpMethod.GET,
            new HttpEntity<>(auth(token)),
            Map.class);
    assertThat(bad.getStatusCode().value()).isEqualTo(400);
  }

  @Test
  @Order(9)
  void 삭제된_옷도_룩에는_계속_보인다() {
    // 룩에 들어간 아이템 하나를 옷장에서 소프트 삭제
    Long deleted = outfitItemIds.get(0);
    rest.exchange(
        "/api/v1/closet-items/" + deleted,
        HttpMethod.DELETE,
        new HttpEntity<>(auth(token)),
        Void.class);

    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/looks/" + lookId, HttpMethod.GET, new HttpEntity<>(auth(token)), Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    List<Number> ids = (List<Number>) res.getBody().get("closetItemIds");
    assertThat(ids.stream().map(Number::longValue)).contains(deleted); // 과거 기록 보존 (계약 §4-6)
  }
}
