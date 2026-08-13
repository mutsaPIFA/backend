package com.mutsapifa.mcmmuse.styling.presentation;

import com.mutsapifa.mcmmuse.shared.aiclient.StyleDnaResult;
import com.mutsapifa.mcmmuse.styling.application.RecommendationService;
import com.mutsapifa.mcmmuse.styling.application.StyleDnaService;
import com.mutsapifa.mcmmuse.styling.application.dto.RecommendationResult;
import com.mutsapifa.mcmmuse.styling.presentation.dto.ClosetItemIdsRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 계약 §4-1(스타일 DNA) · §4-2(MCM 추천) — 화면 4-a·5. 둘 다 미저장(transient). */
@RestController
public class StylingController {

  private final StyleDnaService styleDnaService;
  private final RecommendationService recommendationService;

  public StylingController(
      StyleDnaService styleDnaService, RecommendationService recommendationService) {
    this.styleDnaService = styleDnaService;
    this.recommendationService = recommendationService;
  }

  @PostMapping("/api/v1/style-dna")
  public StyleDnaResult styleDna(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody ClosetItemIdsRequest request) {
    return styleDnaService.analyze(userId, request.closetItemIds());
  }

  @PostMapping("/api/v1/recommendations")
  public RecommendationResult recommendations(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody ClosetItemIdsRequest request) {
    return recommendationService.recommend(userId, request.closetItemIds());
  }
}
