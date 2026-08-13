package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiClosetItem;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선택된 옷장 아이템 검증·조회 공통 로직 (styling → closet 허용 방향).
 *
 * <p>인증·인가 컨벤션: 남의 아이템이 섞이면 403, 유효한 아이템이 하나도 없으면 400.
 */
@Service
@Transactional(readOnly = true)
public class StylingQueryService {

  private final ClosetItemRepository closetItemRepository;

  public StylingQueryService(ClosetItemRepository closetItemRepository) {
    this.closetItemRepository = closetItemRepository;
  }

  public List<AiClosetItem> loadOwnedItems(Long userId, List<Long> closetItemIds) {
    List<ClosetItem> items = closetItemRepository.findAllById(closetItemIds);
    if (items.stream().anyMatch(it -> !it.isOwnedBy(userId))) {
      throw new BusinessException(HttpStatus.FORBIDDEN, "권한이 없습니다");
    }
    List<AiClosetItem> valid =
        items.stream()
            .filter(it -> !it.isDeleted())
            .map(
                it ->
                    new AiClosetItem(
                        it.getId(),
                        it.getCategory(),
                        it.getColor(),
                        it.getMaterial(),
                        it.getMood()))
            .toList();
    if (valid.isEmpty()) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "closetItemIds: 유효한 옷장 아이템이 없습니다");
    }
    return valid;
  }
}
