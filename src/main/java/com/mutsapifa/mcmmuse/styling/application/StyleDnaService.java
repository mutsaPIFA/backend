package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.shared.aiclient.Recommender;
import com.mutsapifa.mcmmuse.shared.aiclient.StyleDnaResult;
import java.util.List;
import org.springframework.stereotype.Service;

/** 계약 §4-1 — 스타일 DNA. 미저장(매번 생성). */
@Service
public class StyleDnaService {

  private final StylingQueryService stylingQueryService;
  private final Recommender recommender;

  public StyleDnaService(StylingQueryService stylingQueryService, Recommender recommender) {
    this.stylingQueryService = stylingQueryService;
    this.recommender = recommender;
  }

  public StyleDnaResult analyze(Long userId, List<Long> closetItemIds) {
    return recommender.styleDna(stylingQueryService.loadOwnedItems(userId, closetItemIds));
  }
}
