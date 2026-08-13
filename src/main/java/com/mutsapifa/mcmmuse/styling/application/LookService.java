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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계약 §4-5~4-7 — 룩 저장(택1)·단건 폴링·목록. 이미지 생성은 비동기(LookImageService). */
@Service
@Transactional
public class LookService {

  private final LookRepository lookRepository;
  private final MoodRepository moodRepository;
  private final McmProductRepository mcmProductRepository;
  private final ClosetItemRepository closetItemRepository;
  private final LookImageService lookImageService;

  public LookService(
      LookRepository lookRepository,
      MoodRepository moodRepository,
      McmProductRepository mcmProductRepository,
      ClosetItemRepository closetItemRepository,
      LookImageService lookImageService) {
    this.lookRepository = lookRepository;
    this.moodRepository = moodRepository;
    this.mcmProductRepository = mcmProductRepository;
    this.closetItemRepository = closetItemRepository;
    this.lookImageService = lookImageService;
  }

  /** 저장 즉시 201 — 이미지 생성은 백그라운드 (계약 D2). wornDate 미전송 시 오늘 (D9). */
  public LookResult save(
      Long userId,
      Long moodId,
      List<Long> closetItemIds,
      Long mcmProductId,
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

    Look look =
        lookRepository.save(
            new Look(
                userId,
                wornDate != null ? wornDate : LocalDate.now(),
                moodId,
                mcmProductId,
                reason,
                closetItemIds));

    lookImageService.generateAsync(look.getId()); // 비동기 — 완료 시 generatedImageUrl 채움

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
      looks = lookRepository.findByUserIdOrderByWornDateDesc(userId);
    } else {
      YearMonth ym;
      try {
        ym = YearMonth.parse(month);
      } catch (Exception e) {
        throw new BusinessException(HttpStatus.BAD_REQUEST, "month: yyyy-MM 형식이어야 합니다");
      }
      looks =
          lookRepository.findByUserIdAndWornDateBetweenOrderByWornDateDesc(
              userId, ym.atDay(1), ym.atEndOfMonth());
    }
    return looks.stream().map(l -> LookResult.from(l, occasionLabelOf(l))).toList();
  }

  private String occasionLabelOf(Look look) {
    return moodRepository.findById(look.getMoodId()).map(Mood::occasionLabel).orElse("");
  }
}
