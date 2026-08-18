package com.mutsapifa.mcmmuse.profile.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.profile.domain.WishlistItem;
import com.mutsapifa.mcmmuse.profile.infrastructure.WishlistItemRepository;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계약 §5-3~5-5 — 찜. add/remove 모두 멱등. */
@Service
public class WishlistService {

  private final WishlistItemRepository wishlistItemRepository;
  private final McmProductRepository mcmProductRepository;

  public WishlistService(
      WishlistItemRepository wishlistItemRepository, McmProductRepository mcmProductRepository) {
    this.wishlistItemRepository = wishlistItemRepository;
    this.mcmProductRepository = mcmProductRepository;
  }

  /** §5-3 — 최근 찜 순 제품 목록 */
  @Transactional(readOnly = true)
  public List<McmProduct> list(Long userId) {
    List<WishlistItem> items = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    List<Long> ids = items.stream().map(WishlistItem::getMcmProductId).toList();
    Map<Long, McmProduct> byId =
        mcmProductRepository.findAllById(ids).stream()
            .collect(java.util.stream.Collectors.toMap(McmProduct::getId, Function.identity()));
    return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
  }

  /** §5-4 — true=새로 찜(201), false=이미 찜(200) */
  @Transactional
  public boolean add(Long userId, Long mcmProductId) {
    if (!mcmProductRepository.existsById(mcmProductId)) {
      throw new BusinessException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다");
    }
    if (wishlistItemRepository.findByUserIdAndMcmProductId(userId, mcmProductId).isPresent()) {
      return false;
    }
    try {
      wishlistItemRepository.save(new WishlistItem(userId, mcmProductId));
      return true;
    } catch (DataIntegrityViolationException e) {
      // 동시 요청으로 유니크 충돌 — 멱등 처리
      return false;
    }
  }

  /** §5-5 — 없어도 204 */
  @Transactional
  public void remove(Long userId, Long mcmProductId) {
    wishlistItemRepository
        .findByUserIdAndMcmProductId(userId, mcmProductId)
        .ifPresent(wishlistItemRepository::delete);
  }
}
