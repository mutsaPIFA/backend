package com.mutsapifa.mcmmuse.profile.presentation;

import com.mutsapifa.mcmmuse.auth.domain.User;
import com.mutsapifa.mcmmuse.auth.presentation.dto.MeResponse;
import com.mutsapifa.mcmmuse.profile.application.ProfileService;
import com.mutsapifa.mcmmuse.profile.presentation.dto.AvatarResponse;
import com.mutsapifa.mcmmuse.profile.presentation.dto.NicknameUpdateRequest;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 계약 §5-1(닉네임) · §5-2(프로필 이미지) — 화면 17. */
@RestController
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  /** §5-1 — 응답은 §1-5와 동일 구조 */
  @PatchMapping("/api/v1/me")
  public MeResponse rename(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody NicknameUpdateRequest request) {
    User user = profileService.rename(userId, request.nickname());
    return MeResponse.of(user, profileService.styleDnaOf(userId));
  }

  /** §5-2 — 업로드 즉시 교체 */
  @PostMapping("/api/v1/me/avatar")
  public AvatarResponse changeAvatar(
      @AuthenticationPrincipal Long userId, @RequestPart("image") MultipartFile image) {
    try {
      return new AvatarResponse(
          profileService.changeAvatar(userId, image.getBytes(), image.getContentType()));
    } catch (IOException e) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지입니다");
    }
  }
}
