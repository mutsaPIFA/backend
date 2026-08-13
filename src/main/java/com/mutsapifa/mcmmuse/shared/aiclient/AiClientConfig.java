package com.mutsapifa.mcmmuse.shared.aiclient;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * AI 포트 배선 — {@code app.ai.{cutout|tagging|standardize}=mock|http} 포트별 스위치.
 *
 * <p>준비된 것부터 실물 전환: 누끼(rembg)는 ai 서비스만 띄우면 http 가능, 태깅·표준화는 Gemini 키(billing) 확보 후.
 */
@Configuration
public class AiClientConfig {

  @Bean
  public WebClient aiWebClient(AiProperties properties) {
    Duration timeout = properties.timeout() != null ? properties.timeout() : Duration.ofSeconds(60);
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
            .responseTimeout(timeout); // Gemini 첫 콜 스로틀(~79s 실측) 대비 — yml에서 조정
    return WebClient.builder()
        .baseUrl(properties.baseUrl())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024)) // 이미지 응답 32MB
        .build();
  }

  @Bean
  public BackgroundRemover backgroundRemover(AiProperties properties, WebClient aiWebClient) {
    return properties.cutoutHttp()
        ? new HttpAiClients.HttpBackgroundRemover(aiWebClient)
        : new MockAiClients.MockBackgroundRemover();
  }

  @Bean
  public VisionTagger visionTagger(AiProperties properties, WebClient aiWebClient) {
    return properties.taggingHttp()
        ? new HttpAiClients.HttpVisionTagger(aiWebClient)
        : new MockAiClients.MockVisionTagger();
  }

  @Bean
  public ImageStandardizer imageStandardizer(AiProperties properties, WebClient aiWebClient) {
    return properties.standardizeHttp()
        ? new HttpAiClients.HttpImageStandardizer(aiWebClient)
        : new MockAiClients.MockImageStandardizer();
  }
}
