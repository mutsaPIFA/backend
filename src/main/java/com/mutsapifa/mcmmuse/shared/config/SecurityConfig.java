package com.mutsapifa.mcmmuse.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mutsapifa.mcmmuse.shared.exception.ApiError;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** 인증 없이 여는 경로 (계약 "공개 엔드포인트" + 개발 도구) */
  private static final String[] PUBLIC_PATHS = {
    "/api/v1/auth/**", "/images/**", "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**"
  };

  private final CorsProperties corsProperties;

  public SecurityConfig(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC_PATHS).permitAll().anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedEntryPoint(objectMapper)))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  /** 401을 계약 형식 {status, message}로 내려준다 (기본 Security 응답은 body 없음). */
  private AuthenticationEntryPoint unauthorizedEntryPoint(ObjectMapper objectMapper) {
    return (request, response, ex) -> {
      response.setStatus(401);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(objectMapper.writeValueAsString(ApiError.of(401, "인증이 필요합니다")));
    };
  }

  /** refresh 쿠키 때문에 allowCredentials=true — 와일드카드 오리진 불가, 명시 목록만. */
  private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(corsProperties.allowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
