package com.mutsapifa.mcmmuse.closet.application;

import com.mutsapifa.mcmmuse.closet.application.dto.ScanResult;
import com.mutsapifa.mcmmuse.closet.domain.exception.InvalidImageException;
import com.mutsapifa.mcmmuse.closet.domain.exception.ScanFailedException;
import com.mutsapifa.mcmmuse.shared.aiclient.BackgroundRemover;
import com.mutsapifa.mcmmuse.shared.aiclient.ImageStandardizer;
import com.mutsapifa.mcmmuse.shared.aiclient.ScanTags;
import com.mutsapifa.mcmmuse.shared.aiclient.VisionTagger;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 옷 스캔 파이프라인 (계약 §3-1, 미저장) — 검증된 순서(research-vision-llm.md §2):
 *
 * <pre>
 * 원본 저장 → ① 표준화(상품컷 재생성) → ② 누끼 → ③ 태깅 → {originalUrl, cutoutUrl, tags}
 * </pre>
 *
 * <p>①이 실패하면 원본으로 폴백(품질만 저하, 스캔은 성공). ②·③ 실패는 409 — '재스캔' = 재호출.
 */
@Slf4j
@Service
public class ScanService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png"); // HEIC는 프론트가 변환 후 업로드 (계약 §3-1)

  private final StorageService storageService;
  private final ImageStandardizer imageStandardizer;
  private final BackgroundRemover backgroundRemover;
  private final VisionTagger visionTagger;

  public ScanService(
      StorageService storageService,
      ImageStandardizer imageStandardizer,
      BackgroundRemover backgroundRemover,
      VisionTagger visionTagger) {
    this.storageService = storageService;
    this.imageStandardizer = imageStandardizer;
    this.backgroundRemover = backgroundRemover;
    this.visionTagger = visionTagger;
  }

  public ScanResult scan(byte[] image, String contentType) {
    if (image == null || image.length == 0 || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new InvalidImageException();
    }

    String originalKey =
        storageService.store(image, "scan", "image/png".equals(contentType) ? "png" : "jpg");

    // ① 표준화 — 실패해도 스캔을 죽이지 않는다 (원본 폴백)
    byte[] standardized;
    try {
      standardized = imageStandardizer.standardize(image);
    } catch (Exception e) {
      log.warn("standardize failed — falling back to original", e);
      standardized = image;
    }

    // ② 누끼 + ③ 태깅 — 실패는 409 (재스캔 유도)
    try {
      byte[] cutout = backgroundRemover.remove(standardized);
      String cutoutKey = storageService.store(cutout, "scan", "png");
      ScanTags tags = visionTagger.tag(standardized);
      return new ScanResult(
          storageService.resolveUrl(originalKey), storageService.resolveUrl(cutoutKey), tags);
    } catch (Exception e) {
      log.error("scan pipeline failed", e);
      throw new ScanFailedException(e);
    }
  }
}
