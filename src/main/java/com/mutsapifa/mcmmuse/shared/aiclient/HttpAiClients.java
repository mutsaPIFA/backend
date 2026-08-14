package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ai 서비스(FastAPI) HTTP 구현 모음 — 내부 계약: ai 레포 docs/internal-api.md.
 *
 * <p>실패 시 예외를 그대로 던진다 — 폴백 판단(표준화 스킵 등)은 호출부(ScanService)의 몫.
 */
public final class HttpAiClients {

  private HttpAiClients() {}

  private static BodyInserters.MultipartInserter imagePart(byte[] image) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder
        .part(
            "image",
            new ByteArrayResource(image) {
              @Override
              public String getFilename() {
                return "image.jpg"; // FastAPI UploadFile은 filename이 있어야 파일 파트로 인식
              }
            })
        .contentType(MediaType.APPLICATION_OCTET_STREAM);
    return BodyInserters.fromMultipartData(builder.build());
  }

  /** POST /cutout → image/png 바이너리 (rembg — 실측 1.6s/장) */
  public static class HttpBackgroundRemover implements BackgroundRemover {
    private final WebClient client;

    public HttpBackgroundRemover(WebClient client) {
      this.client = client;
    }

    @Override
    public byte[] remove(byte[] image) {
      return client
          .post()
          .uri("/cutout")
          .body(imagePart(image))
          .retrieve()
          .bodyToMono(byte[].class)
          .block();
    }
  }

  /** POST /vision/standardize → 상품컷 이미지 바이너리 (Gemini — billing 키 필요) */
  public static class HttpImageStandardizer implements ImageStandardizer {
    private final WebClient client;

    public HttpImageStandardizer(WebClient client) {
      this.client = client;
    }

    @Override
    public byte[] standardize(byte[] image) {
      return client
          .post()
          .uri("/vision/standardize")
          .body(imagePart(image))
          .retrieve()
          .bodyToMono(byte[].class)
          .block();
    }
  }

  /** POST /style-dna·/recommend → 텍스트 LLM. 응답 id·enum은 신뢰하지 않는다 — enum은 엄격 파싱, id는 호출부가 DB 재검증. */
  public static class HttpRecommender implements Recommender {

    private record DnaResponse(
        String summary,
        List<String> dominantColors,
        List<String> dominantMoods,
        List<String> keywords) {}

    private record PickResponse(Long productId, String reason, List<Long> pairsWithItemIds) {}

    private record RecommendResponse(List<PickResponse> picks) {}

    private final WebClient client;

    public HttpRecommender(WebClient client) {
      this.client = client;
    }

    @Override
    public StyleDnaResult styleDna(List<AiClosetItem> items) {
      DnaResponse res =
          client
              .post()
              .uri("/style-dna")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(Map.of("items", items))
              .retrieve()
              .bodyToMono(DnaResponse.class)
              .block();
      return new StyleDnaResult(
          res.summary(),
          res.dominantColors().stream().map(Color::valueOf).toList(),
          res.dominantMoods().stream().map(ItemMood::valueOf).toList(),
          res.keywords());
    }

    @Override
    public List<RecommendationPick> recommend(List<AiClosetItem> items, List<AiProduct> candidates) {
      RecommendResponse res =
          client
              .post()
              .uri("/recommend")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(Map.of("items", items, "products", candidates))
              .retrieve()
              .bodyToMono(RecommendResponse.class)
              .block();
      return res.picks().stream()
          .map(p -> new RecommendationPick(p.productId(), p.reason(), p.pairsWithItemIds()))
          .toList();
    }
  }

  /** POST /outfits → 코디 조합 (상황 Context는 ai 서비스가 무드 라벨로 매핑) */
  public static class HttpOutfitComposer implements OutfitComposer {

    private record LookResponse(
        String concept, List<Long> closetItemIds, Long mcmProductId, String reason) {}

    private record OutfitsResponse(List<LookResponse> looks) {}

    private final WebClient client;

    public HttpOutfitComposer(WebClient client) {
      this.client = client;
    }

    @Override
    public List<OutfitPick> compose(
        String moodLabel, List<AiClosetItem> ownItems, List<AiProduct> mcmCandidates) {
      OutfitsResponse res =
          client
              .post()
              .uri("/outfits")
              .contentType(MediaType.APPLICATION_JSON)
              .bodyValue(Map.of("mood", moodLabel, "items", ownItems, "products", mcmCandidates))
              .retrieve()
              .bodyToMono(OutfitsResponse.class)
              .block();
      return res.looks().stream()
          .map(l -> new OutfitPick(l.closetItemIds(), l.mcmProductId(), l.concept(), l.reason()))
          .toList();
    }
  }

  /** POST /outfits/image → flat-lay 화보 바이너리 (실측 14~31s/장 — 호출부가 후보별 병렬 처리) */
  public static class HttpOutfitImageGenerator implements OutfitImageGenerator {
    private final WebClient client;

    public HttpOutfitImageGenerator(WebClient client) {
      this.client = client;
    }

    @Override
    public byte[] generate(List<byte[]> images) {
      MultipartBodyBuilder builder = new MultipartBodyBuilder();
      for (byte[] image : images) {
        builder
            .part(
                "images",
                new ByteArrayResource(image) {
                  @Override
                  public String getFilename() {
                    return "image.png";
                  }
                })
            .contentType(MediaType.APPLICATION_OCTET_STREAM);
      }
      return client
          .post()
          .uri("/outfits/image")
          .body(BodyInserters.fromMultipartData(builder.build()))
          .retrieve()
          .bodyToMono(byte[].class)
          .block();
    }
  }

  /** POST /vision/tag → {category, color, material, mood} (한국어 vocab 값) */
  public static class HttpVisionTagger implements VisionTagger {

    private record TagResponse(String category, String color, String material, String mood) {}

    private final WebClient client;

    public HttpVisionTagger(WebClient client) {
      this.client = client;
    }

    @Override
    public ScanTags tag(byte[] image) {
      TagResponse res =
          client
              .post()
              .uri("/vision/tag")
              .body(imagePart(image))
              .retrieve()
              .bodyToMono(TagResponse.class)
              .block();
      // vocabulary 밖 값은 여기서 터진다(IllegalArgumentException) → ScanService가 409로 변환.
      // AI 응답은 신뢰 대상이 아니므로(계약 원칙) 관대한 보정 없이 엄격 파싱한다.
      return new ScanTags(
          Category.valueOf(res.category()),
          Color.valueOf(res.color()),
          Material.valueOf(res.material()),
          ItemMood.valueOf(res.mood()));
    }
  }
}
