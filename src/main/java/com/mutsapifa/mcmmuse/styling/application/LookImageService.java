package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.LookImageGenerator;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import com.mutsapifa.mcmmuse.styling.domain.Look;
import com.mutsapifa.mcmmuse.styling.infrastructure.LookRepository;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 룩 이미지 비동기 생성 (계약 D2) — 실패해도 룩 저장은 이미 성공(201), generatedImageUrl만 null로 남는다.
 *
 * <p>mock 상태에선 generator가 null을 반환해 조용히 스킵된다. Gemini http 전환 시 실물 동작.
 */
@Slf4j
@Service
public class LookImageService {

  private final LookRepository lookRepository;
  private final ClosetItemRepository closetItemRepository;
  private final McmProductRepository mcmProductRepository;
  private final LookImageGenerator lookImageGenerator;
  private final StorageService storageService;

  public LookImageService(
      LookRepository lookRepository,
      ClosetItemRepository closetItemRepository,
      McmProductRepository mcmProductRepository,
      LookImageGenerator lookImageGenerator,
      StorageService storageService) {
    this.lookRepository = lookRepository;
    this.closetItemRepository = closetItemRepository;
    this.mcmProductRepository = mcmProductRepository;
    this.lookImageGenerator = lookImageGenerator;
    this.storageService = storageService;
  }

  @Async
  @Transactional
  public void generateAsync(Long lookId) {
    try {
      Look look = lookRepository.findById(lookId).orElse(null);
      if (look == null) {
        return;
      }
      List<String> cutouts =
          closetItemRepository.findAllById(look.getClosetItemIds()).stream()
              .map(it -> it.getCutoutUrl() != null ? it.getCutoutUrl() : it.getImageUrl())
              .filter(Objects::nonNull)
              .toList();
      String mcmImage =
          mcmProductRepository
              .findById(look.getMcmProductId())
              .map(p -> p.getCutoutUrl() != null ? p.getCutoutUrl() : p.getImageUrl())
              .orElse(null);

      byte[] image = lookImageGenerator.generate(cutouts, mcmImage);
      if (image == null) {
        return; // mock 또는 생성 실패 — null 유지 (프론트 플레이스홀더)
      }
      String key = storageService.store(image, "looks", "png");
      look.assignGeneratedImage(storageService.resolveUrl(key));
      lookRepository.save(look);
    } catch (Exception e) {
      log.warn("look image generation failed for look {}", lookId, e); // 저장은 이미 성공 — 조용히 실패
    }
  }
}
