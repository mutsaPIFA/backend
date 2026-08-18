package com.mutsapifa.mcmmuse.closet.presentation;

import com.mutsapifa.mcmmuse.closet.application.ClosetItemService;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.presentation.dto.ClosetItemEditRequest;
import com.mutsapifa.mcmmuse.closet.presentation.dto.ClosetItemRegisterRequest;
import com.mutsapifa.mcmmuse.closet.presentation.dto.ClosetItemResponse;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/closet-items")
public class ClosetItemController {

  private final ClosetItemService closetItemService;

  public ClosetItemController(ClosetItemService closetItemService) {
    this.closetItemService = closetItemService;
  }

  /** 계약 §3-2(스캔 결과) / §3-3(카탈로그 담기) — mcmProductId 유무로 분기 */
  @PostMapping
  public ResponseEntity<ClosetItemResponse> register(
      @AuthenticationPrincipal Long userId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @io.swagger.v3.oas.annotations.media.Content(
                      examples = {
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "스캔 결과 등록",
                            value =
                                "{\"source\":\"OWN\",\"category\":\"상의\",\"color\":\"그레이\","
                                    + "\"material\":\"면\",\"mood\":\"캐주얼\","
                                    + "\"imageUrl\":\"<스캔 응답의 originalUrl>\","
                                    + "\"cutoutUrl\":\"<스캔 응답의 cutoutUrl>\"}"),
                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                            name = "카탈로그 담기",
                            value = "{\"mcmProductId\":12}")
                      }))
          @RequestBody
          ClosetItemRegisterRequest request) {
    ClosetItem item;
    if (request.isCatalogAdd() && request.hasScanFields()) {
      // 두 모드의 body가 섞이면 어느 쪽 의도인지 알 수 없다 — 담기로 오인해 404 내는 것보다 명확한 400
      throw new BusinessException(
          HttpStatus.BAD_REQUEST, "mcmProductId는 카탈로그 담기 전용입니다 — 스캔 결과 등록과 함께 보낼 수 없습니다");
    }
    if (request.isCatalogAdd()) {
      item = closetItemService.registerFromCatalog(userId, request.mcmProductId());
    } else if (request.hasScanFields()) {
      item =
          closetItemService.register(
              userId,
              request.source(),
              request.category(),
              request.color(),
              request.material(),
              request.mood(),
              request.imageUrl(),
              request.cutoutUrl());
    } else {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "값이 올바르지 않습니다");
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(ClosetItemResponse.from(item));
  }

  /** 계약 §3-4 — 목록 (source 미지정 시 전체, createdAt DESC) */
  @GetMapping
  public List<ClosetItemResponse> list(
      @AuthenticationPrincipal Long userId, @RequestParam(required = false) Source source) {
    return closetItemService.list(userId, source).stream().map(ClosetItemResponse::from).toList();
  }

  /** 계약 §3-6 — 부분 수정 (명칭·태그) */
  @PatchMapping("/{id}")
  public ClosetItemResponse edit(
      @AuthenticationPrincipal Long userId,
      @PathVariable Long id,
      @Valid @RequestBody ClosetItemEditRequest request) {
    return ClosetItemResponse.from(
        closetItemService.edit(
            userId,
            id,
            request.name(),
            request.category(),
            request.color(),
            request.material(),
            request.mood()));
  }

  /** 계약 §3-5 — 소프트 삭제 */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
    closetItemService.delete(userId, id);
    return ResponseEntity.noContent().build();
  }
}
