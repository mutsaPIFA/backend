package com.mutsapifa.mcmmuse.profile.application;

import com.mutsapifa.mcmmuse.auth.domain.User;
import com.mutsapifa.mcmmuse.auth.infrastructure.UserRepository;
import com.mutsapifa.mcmmuse.profile.domain.UserStyleDna;
import com.mutsapifa.mcmmuse.profile.infrastructure.UserStyleDnaRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.StyleDnaResult;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.shared.storage.StorageService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계약 §5-1·§5-2 + §1-5 styleDna 스냅샷 관리. */
@Service
public class ProfileService {

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

  private final UserRepository userRepository;
  private final UserStyleDnaRepository userStyleDnaRepository;
  private final StorageService storageService;

  public ProfileService(
      UserRepository userRepository,
      UserStyleDnaRepository userStyleDnaRepository,
      StorageService storageService) {
    this.userRepository = userRepository;
    this.userStyleDnaRepository = userStyleDnaRepository;
    this.storageService = storageService;
  }

  /** §5-1 닉네임 수정 */
  @Transactional
  public User rename(Long userId, String nickname) {
    User user = getUser(userId);
    user.rename(nickname.trim());
    return user;
  }

  /** §5-2 프로필 이미지 — 업로드 즉시 교체 */
  @Transactional
  public String changeAvatar(Long userId, byte[] image, String contentType) {
    if (image == null || image.length == 0 || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지입니다");
    }
    User user = getUser(userId);
    String key = storageService.store(image, "avatars", EXTENSIONS.get(contentType));
    String url = storageService.resolveUrl(key);
    user.changeAvatar(url);
    return url;
  }

  /** §4-1 성공 부수효과 — 최근 DNA 스냅샷 upsert (§1-5 styleDna) */
  @Transactional
  public void saveStyleDna(Long userId, StyleDnaResult result) {
    List<String> colors = result.dominantColors().stream().map(Enum::name).toList();
    List<String> moods = result.dominantMoods().stream().map(Enum::name).toList();
    Instant now = Instant.now();
    userStyleDnaRepository
        .findById(userId)
        .ifPresentOrElse(
            dna -> dna.refresh(result.summary(), colors, moods, result.keywords(), now),
            () ->
                userStyleDnaRepository.save(
                    new UserStyleDna(
                        userId, result.summary(), colors, moods, result.keywords(), now)));
  }

  @Transactional(readOnly = true)
  public UserStyleDna styleDnaOf(Long userId) {
    return userStyleDnaRepository.findById(userId).orElse(null);
  }

  private User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
  }
}
