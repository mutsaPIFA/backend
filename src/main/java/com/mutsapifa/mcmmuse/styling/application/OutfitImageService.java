package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.shared.aiclient.AiProperties;
import com.mutsapifa.mcmmuse.shared.aiclient.OutfitImageGenerator;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import com.mutsapifa.mcmmuse.styling.application.dto.OutfitResult;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 계약 §4-4 — 후보별 AI 화보 생성·저장 (트랜잭션 밖, 후보 병렬).
 *
 * <p>실패 처리: 일부 실패 → 그 후보를 응답에서 제외(성공분만 반환 — 화면은 항상 화보로 일관, 번호는 배열 순서로 자연 재배정). 전부 실패 → 503
 * {@code OUTFIT_GENERATION_FAILED}("다시 시도"). 화보 비활성(mock)일 땐 후보를 그대로 통과시킨다 — 실패가 아니라 기능 꺼짐.
 */
@Slf4j
@Service
public class OutfitImageService {

  // 후보 최대 3 × 소수 동시 사용자 — 데모 규모 전용. 생성 1장 실측 14~31초.
  private final ExecutorService executor = Executors.newFixedThreadPool(6);

  private final OutfitImageGenerator outfitImageGenerator;
  private final StorageService storageService;
  private final AiProperties aiProperties;

  public OutfitImageService(
      OutfitImageGenerator outfitImageGenerator,
      StorageService storageService,
      AiProperties aiProperties) {
    this.outfitImageGenerator = outfitImageGenerator;
    this.storageService = storageService;
    this.aiProperties = aiProperties;
  }

  public List<OutfitResult> attachImages(List<OutfitResult> outfits) {
    if (outfits.isEmpty() || !aiProperties.outfitImageHttp()) {
      return outfits; // 화보 비활성(mock) — imageUrl=null 그대로 (테스트·키 없는 개발 환경)
    }
    List<CompletableFuture<OutfitResult>> futures =
        outfits.stream()
            .map(o -> CompletableFuture.supplyAsync(() -> o.withImageUrl(generate(o)), executor))
            .toList();
    List<OutfitResult> succeeded =
        futures.stream()
            .map(CompletableFuture::join)
            .filter(o -> Objects.nonNull(o.imageUrl()))
            .toList();
    if (succeeded.isEmpty()) {
      // 사실상 ai 서비스 전체 장애 — 후보 없이 명시적 재시도 유도 (계약 §4-4)
      throw new BusinessException(
          HttpStatus.SERVICE_UNAVAILABLE, "코디 생성에 실패했어요. 다시 시도해 주세요.", "OUTFIT_GENERATION_FAILED");
    }
    if (succeeded.size() < outfits.size()) {
      log.warn("후보 {}개 중 {}개 화보 실패 — 응답에서 제외", outfits.size(), outfits.size() - succeeded.size());
    }
    return succeeded;
  }

  /** 실패 시 null — 후보 자체는 살린다. */
  private String generate(OutfitResult outfit) {
    try {
      List<byte[]> images = new ArrayList<>();
      for (OutfitResult.ItemSummary item : outfit.closetItems()) {
        byte[] data = loadImage(item.cutoutUrl());
        if (data != null) {
          images.add(data);
        }
      }
      byte[] mcm =
          loadImage(
              outfit.mcmProduct().cutoutUrl() != null
                  ? outfit.mcmProduct().cutoutUrl()
                  : outfit.mcmProduct().imageUrl());
      if (mcm != null) {
        images.add(mcm); // MCM 정확히 1개 포함 원칙 — 화보에도 반드시 실린다
      }
      if (images.size() < 2) {
        return null;
      }
      byte[] generated = outfitImageGenerator.generate(images);
      if (generated == null) {
        return null; // mock — 폴백 렌더
      }
      String key = storageService.store(generated, "outfits", "png");
      return storageService.resolveUrl(key);
    } catch (Exception e) {
      // 재현용으로 후보 구성까지 남긴다 — 실패는 특정 조합에서만 날 수도 있으므로
      log.warn(
          "outfit image generation failed — imageUrl=null 폴백 (items={}, mcm={}): {}",
          outfit.closetItems().stream().map(OutfitResult.ItemSummary::id).toList(),
          outfit.mcmProduct().id(),
          e.getMessage());
      return null;
    }
  }

  /** 우리 스토리지 URL이면 파일 로드, 외부 URL(MCM CDN 등)이면 HTTP 페치. 실패 시 null. */
  private byte[] loadImage(String url) {
    if (url == null) {
      return null;
    }
    try {
      String key = storageService.keyOf(url);
      if (key != null) {
        return storageService.load(key);
      }
      URLConnection conn = URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(5_000);
      conn.setReadTimeout(10_000);
      try (InputStream in = conn.getInputStream()) {
        return in.readAllBytes();
      }
    } catch (Exception e) {
      log.warn("image load failed — 해당 이미지 제외: {} ({})", url, e.getMessage());
      return null;
    }
  }
}
