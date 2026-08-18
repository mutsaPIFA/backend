package com.mutsapifa.mcmmuse.maintenance;

import com.mutsapifa.mcmmuse.shared.storage.StorageProperties;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 스토리지 고아 파일 정리 — 생성형 파이프라인의 구조적 부산물을 매일 청소한다.
 *
 * <p>후보 화보(§4-4)는 생성 즉시 저장되지만 사용자가 룩으로 저장하지 않으면 아무도 참조하지 않는 고아가 되고, 옷장에 등록하지 않은 스캔 원본·누끼도 같은 방식으로
 * 쌓인다(로컬 실측: 열흘 만에 153MB). DB의 모든 이미지 참조와 디스크 파일을 대조해 미참조 파일을 삭제한다.
 *
 * <p><b>24시간 유예</b>: 방금 생성된 후보 화보는 사용자가 아직 저장 전일 수 있다 — 최근 파일은 건드리지 않는다.
 */
@Service
public class StorageCleanupService {

  private static final Logger log = LoggerFactory.getLogger(StorageCleanupService.class);
  private static final long GRACE_HOURS = 24;

  /** DB에서 이미지 URL을 갖는 모든 컬럼 — 소프트 삭제된 옷장 아이템도 룩 기록이 참조하므로 전부 포함 */
  private static final String REFERENCED_URLS_SQL =
      """
      SELECT image_url FROM closet_items WHERE image_url IS NOT NULL
      UNION SELECT cutout_url FROM closet_items WHERE cutout_url IS NOT NULL
      UNION SELECT generated_image_url FROM looks WHERE generated_image_url IS NOT NULL
      UNION SELECT avatar_url FROM users WHERE avatar_url IS NOT NULL
      UNION SELECT cutout_url FROM mcm_products WHERE cutout_url IS NOT NULL
      """;

  private final EntityManager entityManager;
  private final StorageProperties storageProperties;

  public StorageCleanupService(EntityManager entityManager, StorageProperties storageProperties) {
    this.entityManager = entityManager;
    this.storageProperties = storageProperties;
  }

  /** 부팅 10분 뒤 1회(백필과 겹치지 않게), 이후 24시간마다 */
  @Scheduled(initialDelay = 10 * 60 * 1000L, fixedDelay = 24 * 60 * 60 * 1000L)
  @Transactional(readOnly = true)
  public void cleanOrphans() {
    Path root = Path.of(storageProperties.localPath());
    if (!Files.isDirectory(root)) {
      return;
    }

    Set<String> referencedKeys = referencedKeys();
    Instant cutoff = Instant.now().minus(GRACE_HOURS, ChronoUnit.HOURS);
    AtomicInteger removed = new AtomicInteger();
    AtomicLong removedBytes = new AtomicLong();

    try (Stream<Path> files = Files.walk(root)) {
      files
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                String key = root.relativize(file).toString().replace('\\', '/');
                if (referencedKeys.contains(key)) {
                  return;
                }
                try {
                  if (Files.getLastModifiedTime(file).toInstant().isAfter(cutoff)) {
                    return; // 유예 — 아직 저장 전인 후보 화보일 수 있다
                  }
                  long size = Files.size(file);
                  Files.delete(file);
                  removed.incrementAndGet();
                  removedBytes.addAndGet(size);
                } catch (IOException e) {
                  log.warn("고아 파일 삭제 실패: {}", key, e);
                }
              });
    } catch (IOException e) {
      log.warn("스토리지 정리 순회 실패", e);
      return;
    }

    log.info(
        "스토리지 정리 완료 — 참조 {}건 유지, 고아 {}건 삭제 ({}MB 회수)",
        referencedKeys.size(),
        removed.get(),
        removedBytes.get() / (1024 * 1024));
  }

  /** URL → 스토리지 키("scan/xxx.png"). 외부 URL(CDN 등)은 "/images/"가 없어 자연히 제외된다. */
  private Set<String> referencedKeys() {
    @SuppressWarnings("unchecked")
    List<String> urls = entityManager.createNativeQuery(REFERENCED_URLS_SQL).getResultList();
    Set<String> keys = new HashSet<>();
    for (String url : urls) {
      int idx = url.indexOf("/images/");
      if (idx >= 0) {
        keys.add(url.substring(idx + "/images/".length()));
      }
    }
    return keys;
  }
}
