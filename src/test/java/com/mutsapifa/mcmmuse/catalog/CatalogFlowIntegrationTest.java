package com.mutsapifa.mcmmuse.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.mutsapifa.mcmmuse.catalog.application.McmProductIngestService;
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

/** catalog — 시드 자동 적재(146건)·upsert 멱등·비활성·조회 API (계약 §2). */
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
class CatalogFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired TestRestTemplate rest;
  @Autowired McmProductRepository mcmProductRepository;
  @Autowired McmProductIngestService ingestService;

  static String token;
  static Long anyId;

  private HttpHeaders auth() {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(token);
    return h;
  }

  @Test
  @Order(1)
  void 시드_자동적재_146건과_분포() {
    token =
        (String)
            rest.postForEntity(
                    "/api/v1/auth/register",
                    Map.of("email", "cat-a@test.com", "password", "password123", "nickname", "cat"),
                    Map.class)
                .getBody()
                .get("accessToken");

    ResponseEntity<List> res =
        rest.exchange("/api/v1/mcm-products", HttpMethod.GET, new HttpEntity<>(auth()), List.class);

    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).hasSizeGreaterThanOrEqualTo(146);
    List<String> categories =
        ((List<Map>) res.getBody())
            .stream().map(m -> (String) m.get("category")).distinct().toList();
    assertThat(categories).contains("가방", "악세서리", "신발", "상의", "하의", "아우터");
    Map first = (Map) res.getBody().get(0);
    assertThat(first)
        .containsKeys(
            "id",
            "name",
            "category",
            "color",
            "material",
            "price",
            "imageUrl",
            "cutoutUrl",
            "productUrl");
    anyId = ((Number) first.get("id")).longValue();
  }

  @Test
  @Order(2)
  void query_category_필터() {
    ResponseEntity<List> byName =
        rest.exchange(
            "/api/v1/mcm-products?query={q}",
            HttpMethod.GET,
            new HttpEntity<>(auth()),
            List.class,
            "tracy");
    assertThat(byName.getBody()).isNotEmpty();
    assertThat(((List<Map>) byName.getBody()))
        .allSatisfy(m -> assertThat(((String) m.get("name")).toLowerCase()).contains("tracy"));

    ResponseEntity<List> byCategory =
        rest.exchange(
            "/api/v1/mcm-products?category={c}",
            HttpMethod.GET,
            new HttpEntity<>(auth()),
            List.class,
            "가방");
    assertThat(byCategory.getBody()).hasSize(30);
  }

  @Test
  @Order(3)
  void 상세_200_없으면_404() {
    ResponseEntity<Map> res =
        rest.exchange(
            "/api/v1/mcm-products/" + anyId, HttpMethod.GET, new HttpEntity<>(auth()), Map.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat((String) res.getBody().get("productUrl")).startsWith("https://");

    ResponseEntity<Map> missing =
        rest.exchange(
            "/api/v1/mcm-products/999999", HttpMethod.GET, new HttpEntity<>(auth()), Map.class);
    assertThat(missing.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  @Order(4)
  void 재적재_멱등_신규0() {
    long before = mcmProductRepository.count();
    McmProductIngestService.IngestSummary summary = ingestService.ingest();
    assertThat(summary.inserted()).isZero();
    assertThat(summary.updated()).isGreaterThanOrEqualTo(146);
    assertThat(mcmProductRepository.count()).isEqualTo(before);
  }

  @Test
  @Order(5)
  void 시드에서_빠진_상품은_비활성_상세는_열림() {
    Long extraId =
        mcmProductRepository
            .save(
                new McmProduct(
                    "NOTINSEED01",
                    "단종 상품",
                    Category.가방,
                    Color.블랙,
                    Material.가죽,
                    100000,
                    "https://img/x.jpg",
                    "https://mcm/x",
                    null,
                    null,
                    List.of(),
                    null,
                    null))
            .getId();

    ingestService.ingest(); // 시드에 없으므로 비활성 처리

    ResponseEntity<List> list =
        rest.exchange("/api/v1/mcm-products", HttpMethod.GET, new HttpEntity<>(auth()), List.class);
    assertThat(((List<Map>) list.getBody()))
        .noneSatisfy(m -> assertThat(m.get("name")).isEqualTo("단종 상품")); // 목록에서 숨음

    ResponseEntity<Map> detail =
        rest.exchange(
            "/api/v1/mcm-products/" + extraId, HttpMethod.GET, new HttpEntity<>(auth()), Map.class);
    assertThat(detail.getStatusCode().value()).isEqualTo(200); // 옷장에 담긴 비활성 상품 대비 — 상세는 열림
  }
}
