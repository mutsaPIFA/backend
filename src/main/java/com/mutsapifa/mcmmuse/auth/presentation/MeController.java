package com.mutsapifa.mcmmuse.auth.presentation;

import com.mutsapifa.mcmmuse.auth.application.AuthService;
import com.mutsapifa.mcmmuse.auth.presentation.dto.MeResponse;
import com.mutsapifa.mcmmuse.profile.application.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

  private final AuthService authService;
  private final ProfileService profileService;

  public MeController(AuthService authService, ProfileService profileService) {
    this.authService = authService;
    this.profileService = profileService;
  }

  /** 계약 §1-5 — profile 탭. JwtAuthFilter가 principal에 userId(Long)를 심는다. */
  @GetMapping("/api/v1/me")
  public MeResponse me(@AuthenticationPrincipal Long userId) {
    return MeResponse.of(authService.getUser(userId), profileService.styleDnaOf(userId));
  }
}
