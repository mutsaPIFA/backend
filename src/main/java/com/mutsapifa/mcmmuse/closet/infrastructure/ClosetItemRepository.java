package com.mutsapifa.mcmmuse.closet.infrastructure;

import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ⚠️ 활성 조회는 반드시 {@code DeletedAtIsNull} 메서드를 쓴다 — 없는 조회를 새로 만들 때도 같은 조건을 붙일 것. 필터 없는 {@code
 * findById}·{@code findAllById}는 룩(과거 기록) 조회 전용.
 */
public interface ClosetItemRepository extends JpaRepository<ClosetItem, Long> {

  /** 화면 9 옷장 목록 — 계약: createdAt DESC */
  List<ClosetItem> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

  List<ClosetItem> findByUserIdAndSourceAndDeletedAtIsNullOrderByCreatedAtDesc(
      Long userId, Source source);

  /** 소유권 검사 포함 단건 (활성만) */
  Optional<ClosetItem> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

  /** POST /outfits 선행조건 — 옷장에 MCM 있는지 (없으면 409 NO_MCM_IN_CLOSET) */
  boolean existsByUserIdAndSourceAndDeletedAtIsNull(Long userId, Source source);
}
