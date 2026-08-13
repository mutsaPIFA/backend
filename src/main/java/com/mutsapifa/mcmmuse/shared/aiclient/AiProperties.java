package com.mutsapifa.mcmmuse.shared.aiclient;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 {@code app.ai.*} 바인딩.
 *
 * <p>포트별 스위치("mock" | "http") — 준비된 것부터 실물 전환한다. 예: 누끼(rembg)는 http, 태깅(Gemini)은 키 확보 전까지 mock.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String baseUrl, Duration timeout, String cutout, String tagging, String standardize) {

  public boolean cutoutHttp() {
    return "http".equals(cutout);
  }

  public boolean taggingHttp() {
    return "http".equals(tagging);
  }

  public boolean standardizeHttp() {
    return "http".equals(standardize);
  }
}
