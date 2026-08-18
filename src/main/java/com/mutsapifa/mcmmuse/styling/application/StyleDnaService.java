package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.profile.application.ProfileService;
import com.mutsapifa.mcmmuse.shared.aiclient.Recommender;
import com.mutsapifa.mcmmuse.shared.aiclient.StyleDnaResult;
import java.util.List;
import org.springframework.stereotype.Service;

/** 계약 §4-1 — 스타일 DNA. 응답은 매번 생성, 성공 시 최신 스냅샷을 프로필에 저장(§1-5 styleDna). */
@Service
public class StyleDnaService {

  private final StylingQueryService stylingQueryService;
  private final Recommender recommender;
  private final ProfileService profileService;

  public StyleDnaService(
      StylingQueryService stylingQueryService,
      Recommender recommender,
      ProfileService profileService) {
    this.stylingQueryService = stylingQueryService;
    this.recommender = recommender;
    this.profileService = profileService;
  }

  public StyleDnaResult analyze(Long userId, List<Long> closetItemIds) {
    StyleDnaResult result =
        recommender.styleDna(stylingQueryService.loadOwnedItems(userId, closetItemIds));
    profileService.saveStyleDna(userId, result);
    return result;
  }
}
