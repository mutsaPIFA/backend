package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
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
