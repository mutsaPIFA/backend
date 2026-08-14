package com.mutsapifa.mcmmuse.styling.presentation;

import com.mutsapifa.mcmmuse.styling.application.LookService;
import com.mutsapifa.mcmmuse.styling.application.OutfitImageService;
import com.mutsapifa.mcmmuse.styling.application.OutfitService;
import com.mutsapifa.mcmmuse.styling.application.dto.LookResult;
import com.mutsapifa.mcmmuse.styling.application.dto.OutfitResult;
import com.mutsapifa.mcmmuse.styling.infrastructure.MoodRepository;
import com.mutsapifa.mcmmuse.styling.presentation.dto.LookSaveRequest;
import com.mutsapifa.mcmmuse.styling.presentation.dto.MoodResponse;
import com.mutsapifa.mcmmuse.styling.presentation.dto.OutfitComposeRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 계약 §4-3~4-7 — 큐레이터: 무드·코디 후보·룩 (AI큐레이터~코디기록 화면 트랙). */
@RestController
public class CurationController {

  private final MoodRepository moodRepository;
  private final OutfitService outfitService;
  private final OutfitImageService outfitImageService;
  private final LookService lookService;

  public CurationController(
      MoodRepository moodRepository,
      OutfitService outfitService,
      OutfitImageService outfitImageService,
      LookService lookService) {
    this.moodRepository = moodRepository;
    this.outfitService = outfitService;
    this.outfitImageService = outfitImageService;
    this.lookService = lookService;
  }

  /** §4-3 — 고정 시드 6개 */
  @GetMapping("/api/v1/moods")
  public List<MoodResponse> moods() {
    return moodRepository.findAllByOrderByIdAsc().stream().map(MoodResponse::from).toList();
  }

  /** §4-4 — 코디 후보 최대 3 (미저장) */
  @PostMapping("/api/v1/outfits")
  public List<OutfitResult> outfits(
      @AuthenticationPrincipal Long userId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "무드만", value = "{\"moodId\":1}"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "시드 제품 고정",
                            value = "{\"moodId\":1,\"seedMcmProductId\":12}")
                      }))
          @Valid
          @RequestBody
          OutfitComposeRequest request) {
    // 화보 생성(후보 병렬, 실측 20~40초)은 트랜잭션 밖 — 프론트는 "생성 중" 로딩 연출 (계약 §4-4)
    return outfitImageService.attachImages(
        outfitService.compose(userId, request.moodId(), request.seedMcmProductId()));
  }

  /** §4-5 — 룩 저장 (즉시 201, 이미지는 비동기) */
  @PostMapping("/api/v1/looks")
  public ResponseEntity<LookResult> saveLook(
      @AuthenticationPrincipal Long userId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      examples =
                          @io.swagger.v3.oas.annotations.media.ExampleObject(
                              name = "룩 저장",
                              value =
                                  "{\"moodId\":1,\"closetItemIds\":[1,2],\"mcmProductId\":12,"
                                      + "\"imageUrl\":\"<후보 응답의 imageUrl>\","
                                      + "\"concept\":\"<후보 응답의 concept>\"}")))
          @Valid
          @RequestBody
          LookSaveRequest request) {
    LookResult result =
        lookService.save(
            userId,
            request.moodId(),
            request.closetItemIds(),
            request.mcmProductId(),
            request.imageUrl(),
            request.concept(),
            request.reason(),
            request.wornDate());
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  /** §4-6 — 단건 (이미지 생성 폴링) */
  @GetMapping("/api/v1/looks/{id}")
  public LookResult getLook(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
    return lookService.get(userId, id);
  }

  /** §4-7 — 목록 (month=yyyy-MM) */
  @GetMapping("/api/v1/looks")
  public List<LookResult> listLooks(
      @AuthenticationPrincipal Long userId, @RequestParam(required = false) String month) {
    return lookService.list(userId, month);
  }
}
