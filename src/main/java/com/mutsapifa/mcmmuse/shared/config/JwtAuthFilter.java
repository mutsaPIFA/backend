package com.mutsapifa.mcmmuse.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer} 헤더의 access token을 검증해 SecurityContext에 userId를 심는다.
 *
 * <p>토큰이 없거나 무효면 그냥 통과 — 보호된 경로에서는 entry point가 401을 만든다.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      jwtTokenProvider
          .parseUserId(header.substring(BEARER_PREFIX.length()))
          .ifPresent(
              userId ->
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(userId, null, List.of())));
    }
    filterChain.doFilter(request, response);
  }
}
