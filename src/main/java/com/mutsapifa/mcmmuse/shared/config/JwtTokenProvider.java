package com.mutsapifa.mcmmuse.shared.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Access Token(JWT) 생성·검증.
 *
 * <p>Refresh Token은 JWT가 아니라 불투명 랜덤 문자열 + DB 저장(auth BC 소관)이라 여기 없다. 여기는 모든 요청이 통과하는
 * 검증(cross-cutting)만 담당한다.
 */
@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final JwtProperties properties;

  public JwtTokenProvider(JwtProperties properties) {
    this.properties = properties;
    this.key =
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            properties.secret().getBytes(StandardCharsets.UTF_8));
  }

  /** userId를 subject로 하는 access token 발급. */
  public String createAccessToken(Long userId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.accessTokenTtl())))
        .signWith(key)
        .compact();
  }

  /** 유효하면 userId, 아니면 empty. (만료·위조·형식 오류 전부 empty) */
  public Optional<Long> parseUserId(String token) {
    try {
      String subject =
          Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
      return Optional.of(Long.parseLong(subject));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
