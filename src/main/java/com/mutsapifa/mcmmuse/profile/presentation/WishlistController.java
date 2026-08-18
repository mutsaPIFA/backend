package com.mutsapifa.mcmmuse.profile.presentation;

import com.mutsapifa.mcmmuse.catalog.presentation.dto.McmProductResponse;
import com.mutsapifa.mcmmuse.profile.application.WishlistService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 계약 §5-3~5-5 — 찜. 홈 하트·프로필 찜 목록. */
@RestController
public class WishlistController {

  private final WishlistService wishlistService;

  public WishlistController(WishlistService wishlistService) {
    this.wishlistService = wishlistService;
  }

  /** §5-3 — 최근 찜 순 (2-1 객체 배열) */
  @GetMapping("/api/v1/wishlist")
  public List<McmProductResponse> list(@AuthenticationPrincipal Long userId) {
    return wishlistService.list(userId).stream().map(McmProductResponse::from).toList();
  }

  /** §5-4 — 201, 이미 찜이면 200 (멱등) */
  @PostMapping("/api/v1/wishlist/{mcmProductId}")
  public ResponseEntity<Void> add(
      @AuthenticationPrincipal Long userId, @PathVariable Long mcmProductId) {
    boolean created = wishlistService.add(userId, mcmProductId);
    return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).build();
  }

  /** §5-5 — 없어도 204 (멱등) */
  @DeleteMapping("/api/v1/wishlist/{mcmProductId}")
  public ResponseEntity<Void> remove(
      @AuthenticationPrincipal Long userId, @PathVariable Long mcmProductId) {
    wishlistService.remove(userId, mcmProductId);
    return ResponseEntity.noContent().build();
  }
}
