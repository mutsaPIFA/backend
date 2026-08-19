package com.mutsapifa.mcmmuse.closet.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.domain.exception.ClosetAccessDeniedException;
import com.mutsapifa.mcmmuse.closet.domain.exception.ClosetItemNotFoundException;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClosetItemService {

  private final ClosetItemRepository closetItemRepository;
  private final McmProductRepository mcmProductRepository; // closet → catalog (허용 방향)

  public ClosetItemService(
      ClosetItemRepository closetItemRepository, McmProductRepository mcmProductRepository) {
    this.closetItemRepository = closetItemRepository;
    this.mcmProductRepository = mcmProductRepository;
  }

  /** 계약 §3-2 — 스캔 결과 등록 (태그는 사용자가 수정한 값 그대로, 명칭은 옵션 — §3-6과 같은 의미론) */
  public ClosetItem register(
      Long userId,
      String name,
      Source source,
      Category category,
      Color color,
      Material material,
      ItemMood mood,
      String imageUrl,
      String cutoutUrl) {
    ClosetItem item =
        new ClosetItem(userId, category, color, material, mood, imageUrl, cutoutUrl, source);
    item.edit(name, null, null, null, null);
    return closetItemRepository.save(item);
  }

  /** 계약 §3-3 — 카탈로그 MCM 담기 (태그·이미지는 제품에서 복사) */
  public ClosetItem registerFromCatalog(Long userId, Long mcmProductId) {
    McmProduct product =
        mcmProductRepository
            .findById(mcmProductId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다"));
    // McmProduct에는 mood(아이템 무드 태그)가 없다 — MCM 제품이므로 럭셔리를 기본값으로 복사한다.
    return closetItemRepository.save(
        ClosetItem.fromCatalog(
            userId,
            product.getId(),
            product.getCategory(),
            product.getColor(),
            product.getMaterial(),
            ItemMood.럭셔리,
            product.getImageUrl(),
            product.getCutoutUrl()));
  }

  /** 계약 §3-4 — 목록 (createdAt DESC, 활성만) */
  @Transactional(readOnly = true)
  public List<ClosetItem> list(Long userId, Source source) {
    return source == null
        ? closetItemRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
        : closetItemRepository.findByUserIdAndSourceAndDeletedAtIsNullOrderByCreatedAtDesc(
            userId, source);
  }

  /** 계약 §3-6 — 부분 수정 (명칭·태그). null 필드는 유지, 명칭 빈 문자열은 제거 */
  public ClosetItem edit(
      Long userId,
      Long itemId,
      String name,
      Category category,
      Color color,
      Material material,
      ItemMood mood) {
    ClosetItem item =
        closetItemRepository
            .findById(itemId)
            .filter(it -> !it.isDeleted())
            .orElseThrow(ClosetItemNotFoundException::new);
    if (!item.isOwnedBy(userId)) {
      throw new ClosetAccessDeniedException();
    }
    item.edit(name, category, color, material, mood);
    return item;
  }

  /** 계약 §3-5 — 소프트 삭제. 404(없거나 이미 삭제) / 403(남의 것) 구분 */
  public void delete(Long userId, Long itemId) {
    ClosetItem item =
        closetItemRepository
            .findById(itemId)
            .filter(it -> !it.isDeleted())
            .orElseThrow(ClosetItemNotFoundException::new);
    if (!item.isOwnedBy(userId)) {
      throw new ClosetAccessDeniedException();
    }
    item.delete(Instant.now());
  }
}
