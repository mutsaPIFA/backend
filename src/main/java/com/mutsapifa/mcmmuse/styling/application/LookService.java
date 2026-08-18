package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.styling.application.dto.LookResult;
import com.mutsapifa.mcmmuse.styling.domain.Look;
import com.mutsapifa.mcmmuse.styling.domain.Mood;
import com.mutsapifa.mcmmuse.styling.domain.exception.LookNotFoundException;
import com.mutsapifa.mcmmuse.styling.domain.exception.MoodNotFoundException;
import com.mutsapifa.mcmmuse.styling.infrastructure.LookRepository;
import com.mutsapifa.mcmmuse.styling.infrastructure.MoodRepository;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계약 §4-5~4-7 — 룩 저장(택1)·단건·목록. 화보는 후보(§4-4)에서 생성된 것을 재사용 — 저장 시 재생성 없음. */
@Service
@Transactional
public class LookService {

  private final LookRepository lookRepository;
  private final MoodRepository moodRepository;
  private final McmProductRepository mcmProductRepository;
  private final ClosetItemRepository closetItemRepository;
  private final StorageService storageService;

  public LookService(
      LookRepository lookRepository,
      MoodRepository moodRepository,
      McmProductRepository mcmProductRepository,
      ClosetItemRepository closetItemRepository,
      StorageService storageService) {
    this.lookRepository = lookRepository;
    this.moodRepository = moodRepository;
    this.mcmProductRepository = mcmProductRepository;
    this.closetItemRepository = closetItemRepository;
    this.storageService = storageService;
  }

  /** 저장 즉시 201 + generatedImageUrl 확정 (후보 화보 재사용). wornDate 미전송 시 오늘 (D9). */
  public LookResult save(
      Long userId,
      Long moodId,
      List<Long> closetItemIds,
      Long mcmProductId,
      String imageUrl,
      String concept,
      String note,
      String reason,
      LocalDate wornDate) {
    Mood mood = moodRepository.findById(moodId).orElseThrow(MoodNotFoundException::new);
    mcmProductRepository
        .findById(mcmProductId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다"));

    List<ClosetItem> items = closetItemRepository.findAllById(closetItemIds);
    if (items.stream().anyMatch(it -> !it.isOwnedBy(userId))) {
      throw new BusinessException(HttpStatus.FORBIDDEN, "권한이 없습니다");
    }

    // 화보 URL은 우리 스토리지가 서빙하는 것만 수납 — 임의 외부 URL이 룩에 박히는 것 방지
    if (imageUrl != null && !imageUrl.isBlank() && storageService.keyOf(imageUrl) == null) {
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "imageUrl: 코디 후보 응답의 imageUrl만 사용할 수 있습니다");
    }

    Look look =
        new Look(
            userId,
            wornDate != null ? wornDate : LocalDate.now(),
            moodId,
            mcmProductId,
            concept,
            note,
            reason,
            closetItemIds);
    if (imageUrl != null && !imageUrl.isBlank()) {
      look.assignGeneratedImage(imageUrl);
    }
    look = lookRepository.save(look);

    return LookResult.from(look, mood.occasionLabel());
  }

  /** 폴링용 단건 (계약 §4-6). 룩이 참조하는 아이템은 삭제됐어도 계속 보인다. */
  @Transactional(readOnly = true)
  public LookResult get(Long userId, Long lookId) {
    Look look = lookRepository.findById(lookId).orElseThrow(LookNotFoundException::new);
    if (!look.isOwnedBy(userId)) {
      throw new BusinessException(HttpStatus.FORBIDDEN, "권한이 없습니다");
    }
    return LookResult.from(look, occasionLabelOf(look));
  }

  /** 목록 (계약 §4-7). month=yyyy-MM 필터. */
  @Transactional(readOnly = true)
  public List<LookResult> list(Long userId, String month) {
    List<Look> looks;
    if (month == null || month.isBlank()) {
      looks = lookRepository.findByUserIdOrderByWornDateDescCreatedAtDesc(userId);
    } else {
      YearMonth ym;
      try {
        ym = YearMonth.parse(month);
      } catch (Exception e) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "month: yyyy-MM 형식이어야 합니다");
      }
      looks =
          lookRepository.findByUserIdAndWornDateBetweenOrderByWornDateDescCreatedAtDesc(
              userId, ym.atDay(1), ym.atEndOfMonth());
    }
    return looks.stream().map(l -> LookResult.from(l, occasionLabelOf(l))).toList();
  }

  /** 부분 수정 (계약 §4-9) — 소감·날짜만. null 필드는 유지, 소감 빈 문자열은 제거. */
  @Transactional
  public LookResult edit(Long userId, Long lookId, String note, LocalDate wornDate) {
    Look look = lookRepository.findById(lookId).orElseThrow(LookNotFoundException::new);
    if (!look.isOwnedBy(userId)) {
      throw new BusinessException(HttpStatus.FORBIDDEN, "권한이 없습니다");
    }
    look.edit(note, wornDate);
    return LookResult.from(look, occasionLabelOf(look));
  }

  /** 기록 취소 (계약 §4-8) — 완전 삭제. 취소 후 같은 후보를 다시 기록할 수 있다. */
  @Transactional
  public void delete(Long userId, Long lookId) {
    Look look = lookRepository.findById(lookId).orElseThrow(LookNotFoundException::new);
    if (!look.isOwnedBy(userId)) {
      throw new BusinessException(HttpStatus.FORBIDDEN, "권한이 없습니다");
    }
    lookRepository.delete(look);
  }

  private String occasionLabelOf(Look look) {
    return moodRepository.findById(look.getMoodId()).map(Mood::occasionLabel).orElse("");
  }
}
