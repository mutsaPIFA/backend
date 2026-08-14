package com.mutsapifa.mcmmuse.shared.aiclient;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

  @Bean
  public OutfitImageGenerator outfitImageGenerator(AiProperties properties, WebClient aiWebClient) {
    // mock = 생성 안 함(null) — 후보 imageUrl=null이면 프론트가 누끼 콜라주로 폴백 (계약 §4-4)
    return properties.outfitImageHttp()
        ? new HttpAiClients.HttpOutfitImageGenerator(aiWebClient)
        : images -> null;
  }

  @Bean
  public Recommender recommender(AiProperties properties, WebClient aiWebClient) {
    MockRecommender fallback = new MockRecommender();
    if (!properties.recommendHttp()) {
      return fallback;
    }
    // LLM 실패 시 룰베이스로 런타임 폴백 — 데모가 Gemini 가용성에 볼모 잡히지 않게
    HttpAiClients.HttpRecommender http = new HttpAiClients.HttpRecommender(aiWebClient);
    return new Recommender() {
      @Override
      public StyleDnaResult styleDna(List<AiClosetItem> items) {
        try {
          return http.styleDna(items);
        } catch (Exception e) {
          log.warn("style-dna LLM 실패 — 룰베이스 폴백: {}", e.getMessage());
          return fallback.styleDna(items);
        }
      }

      @Override
      public List<RecommendationPick> recommend(
          List<AiClosetItem> items, List<AiProduct> candidates) {
        try {
          return http.recommend(items, candidates);
        } catch (Exception e) {
          log.warn("recommend LLM 실패 — 룰베이스 폴백: {}", e.getMessage());
          return fallback.recommend(items, candidates);
        }
      }
    };
  }

  @Bean
  public OutfitComposer outfitComposer(AiProperties properties, WebClient aiWebClient) {
    MockOutfitComposer fallback = new MockOutfitComposer();
    if (!properties.outfitComposeHttp()) {
      return fallback;
    }
    HttpAiClients.HttpOutfitComposer http = new HttpAiClients.HttpOutfitComposer(aiWebClient);
    return (moodLabel, ownItems, mcmCandidates) -> {
      try {
        return http.compose(moodLabel, ownItems, mcmCandidates);
      } catch (Exception e) {
        log.warn("outfits LLM 실패 — 룰베이스 폴백: {}", e.getMessage());
        return fallback.compose(moodLabel, ownItems, mcmCandidates);
      }
    };
  }
}
