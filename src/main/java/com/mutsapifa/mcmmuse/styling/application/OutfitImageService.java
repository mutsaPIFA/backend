package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.shared.aiclient.OutfitImageGenerator;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import com.mutsapifa.mcmmuse.styling.application.dto.OutfitResult;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 계약 §4-4 — 후보별 AI 화보 생성·저장 (트랜잭션 밖, 후보 병렬).
 *
 * <p>후보 하나가 실패해도 나머지는 산다 — 실패한 후보만 {@code imageUrl=null}(프론트 콜라주 폴백). 저장(§4-5)은 여기서 만든 URL을
 * 재사용하므로 룩 저장 시 재생성이 없다.
 */
@Slf4j
@Service
public class OutfitImageService {

  // 후보 최대 3 × 소수 동시 사용자 — 데모 규모 전용. 생성 1장 실측 14~31초.
  private final ExecutorService executor = Executors.newFixedThreadPool(6);

  private final OutfitImageGenerator outfitImageGenerator;
  private final StorageService storageService;

  public OutfitImageService(
      OutfitImageGenerator outfitImageGenerator, StorageService storageService) {
    this.outfitImageGenerator = outfitImageGenerator;
    this.storageService = storageService;
  }

  public List<OutfitResult> attachImages(List<OutfitResult> outfits) {
    List<CompletableFuture<OutfitResult>> futures =
        outfits.stream()
            .map(o -> CompletableFuture.supplyAsync(() -> o.withImageUrl(generate(o)), executor))
            .toList();
    return futures.stream().map(CompletableFuture::join).toList();
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
      log.warn("outfit image generation failed — imageUrl=null 폴백: {}", e.getMessage());
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
