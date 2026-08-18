package com.mutsapifa.mcmmuse.profile.infrastructure;

import com.mutsapifa.mcmmuse.profile.domain.WishlistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

  /** 계약 §5-3 — 최근 찜 순 */
  List<WishlistItem> findByUserIdOrderByCreatedAtDesc(Long userId);

  Optional<WishlistItem> findByUserIdAndMcmProductId(Long userId, Long mcmProductId);
}
