package com.mutsapifa.mcmmuse.auth.application;

import com.mutsapifa.mcmmuse.auth.application.dto.AuthResult;
import com.mutsapifa.mcmmuse.auth.domain.RefreshToken;
import com.mutsapifa.mcmmuse.auth.domain.User;
import com.mutsapifa.mcmmuse.auth.domain.exception.DuplicateEmailException;
import com.mutsapifa.mcmmuse.auth.domain.exception.InvalidCredentialsException;
import com.mutsapifa.mcmmuse.auth.domain.exception.InvalidRefreshTokenException;
import com.mutsapifa.mcmmuse.auth.infrastructure.RefreshTokenRepository;
import com.mutsapifa.mcmmuse.auth.infrastructure.UserRepository;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.config.DemoProperties;
import com.mutsapifa.mcmmuse.shared.config.JwtProperties;
import com.mutsapifa.mcmmuse.shared.config.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  // auth → closet 방향 의존(게스트 시드 전용) — 옷장 복사가 계정 생성과 한 트랜잭션이어야 해서 여기서 소유
  private final ClosetItemRepository closetItemRepository;
  private final DemoProperties demoProperties;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties,
      ClosetItemRepository closetItemRepository,
      DemoProperties demoProperties) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
    this.closetItemRepository = closetItemRepository;
    this.demoProperties = demoProperties;
  }

  public AuthResult register(String email, String password, String nickname) {
    String normalizedEmail = normalize(email);
    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new DuplicateEmailException();
    }
    User user =
        userRepository.save(new User(normalizedEmail, passwordEncoder.encode(password), nickname));
    return issueTokens(user.getId());
  }

  /**
   * §1-6 — 게스트 발급. QR 진입용: 이메일·비밀번호는 내부 생성(사용자에게 노출하지 않음), 옷장은 템플릿 계정
   * 복사로 시드. 게스트 이메일은 {@code @guest.mcmmuse.local} 도메인으로 통일 — 시연 후 일괄 정리용.
   */
  public AuthResult registerGuest() {
    byte[] bytes = new byte[5];
    SECURE_RANDOM.nextBytes(bytes);
    String email = "guest-" + HexFormat.of().formatHex(bytes) + "@guest.mcmmuse.local";
    String nickname = "게스트" + (1000 + SECURE_RANDOM.nextInt(9000));
    User user =
        userRepository.save(
            new User(email, passwordEncoder.encode(generateOpaqueToken()), nickname));
    seedClosetFromTemplate(user.getId());
    return issueTokens(user.getId());
  }

  private void seedClosetFromTemplate(Long guestUserId) {
    String templateEmail = demoProperties.guestTemplateEmail();
    if (templateEmail == null || templateEmail.isBlank()) {
      return;
    }
    userRepository
        .findByEmail(normalize(templateEmail))
        .ifPresent(
            template ->
                closetItemRepository
                    .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(template.getId())
                    .forEach(
                        item -> closetItemRepository.save(ClosetItem.copyOf(guestUserId, item))));
  }

  public AuthResult login(String email, String password) {
    User user =
        userRepository.findByEmail(normalize(email)).orElseThrow(InvalidCredentialsException::new);
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException();
    }
    return issueTokens(user.getId());
  }

  /** 회전: 유효한 refresh를 소모(삭제)하고 새 쌍을 발급한다. */
  public AuthResult refresh(String rawRefreshToken) {
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(sha256(rawRefreshToken))
            .orElseThrow(InvalidRefreshTokenException::new);
    if (stored.isExpired(Instant.now())) {
      refreshTokenRepository.delete(stored);
      throw new InvalidRefreshTokenException();
    }
    refreshTokenRepository.delete(stored);
    return issueTokens(stored.getUserId());
  }

  /** 로그아웃 — 서버에서 refresh row를 삭제해 실제로 무효화한다. 무효 토큰이어도 조용히 성공(204). */
  public void logout(String rawRefreshToken) {
    if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
      refreshTokenRepository.deleteByTokenHash(sha256(rawRefreshToken));
    }
  }

  @Transactional(readOnly = true)
  public User getUser(Long userId) {
    return userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
  }

  private AuthResult issueTokens(Long userId) {
    String accessToken = jwtTokenProvider.createAccessToken(userId);
    String refreshToken = generateOpaqueToken();
    refreshTokenRepository.save(
        new RefreshToken(
            userId, sha256(refreshToken), Instant.now().plus(jwtProperties.refreshTokenTtl())));
    return new AuthResult(userId, accessToken, refreshToken);
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  /** 불투명 랜덤 토큰 (JWT 아님 — DB 해시 대조로만 검증). */
  private static String generateOpaqueToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
