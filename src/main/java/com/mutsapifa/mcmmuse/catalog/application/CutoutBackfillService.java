package com.mutsapifa.mcmmuse.catalog.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiProperties;
import com.mutsapifa.mcmmuse.shared.aiclient.BackgroundRemover;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * MCM 상품 누끼 백필 — cutoutUrl 없는 상품의 이미지를 받아 rembg로 누끼를 떠서 StorageService에 저장.
 *
 * <p>{@code app.ai.cutout=http}일 때만 동작(mock이면 스킵 — 원본 통과 누끼는 무의미). 저장은 StorageService 경유라 로컬=개인
 * 디스크, 배포=S3(팀 공유) — 배포 기준 설계. 실측 기준 146개 ≈ 5분, 비동기라 부팅 안 막음.
 */
@Slf4j
@Service
public class CutoutBackfillService {

  /** MCM CDN이 기본 UA를 차단하는 경우가 있어(파이썬 urllib 403 실측) 브라우저 UA 사용 */
  private static final String USER_AGENT = "Mozilla/5.0";

  private final McmProductRepository mcmProductRepository;
  private final BackgroundRemover backgroundRemover;
  private final StorageService storageService;
  private final AiProperties aiProperties;
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public CutoutBackfillService(
      McmProductRepository mcmProductRepository,
      BackgroundRemover backgroundRemover,
      StorageService storageService,
      AiProperties aiProperties) {
    this.mcmProductRepository = mcmProductRepository;
    this.backgroundRemover = backgroundRemover;
    this.storageService = storageService;
    this.aiProperties = aiProperties;
  }

  @Async
  public void backfillAsync() {
    if (!aiProperties.cutoutHttp()) {
      log.info("누끼 백필 스킵 — app.ai.cutout=mock (rembg 서비스 연결 시 http로 전환)");
      return;
    }
    List<McmProduct> targets =
        mcmProductRepository.findByActiveTrueOrderByIdAsc().stream()
            .filter(p -> p.getCutoutUrl() == null)
            .toList();
    log.info("누끼 백필 시작 — 대상 {}건", targets.size());
    int ok = 0;
    for (McmProduct p : targets) {
      try {
        byte[] original = fetch(p.getImageUrl());
        byte[] cutout = backgroundRemover.remove(original);
        String key = storageService.store(cutout, "catalog", "png");
        p.assignCutout(storageService.resolveUrl(key));
        mcmProductRepository.save(p);
        ok++;
      } catch (Exception e) {
        log.warn("누끼 백필 실패 — sku {}: {}", p.getSku(), e.getMessage()); // null 유지, 다음 기동 때 재시도
      }
    }
    log.info("누끼 백필 완료 — 성공 {}/{}", ok, targets.size());
  }

  private byte[] fetch(String url) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
    HttpResponse<byte[]> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("이미지 다운로드 실패 HTTP " + response.statusCode());
    }
    return response.body();
  }
}
