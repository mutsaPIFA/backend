package com.mutsapifa.mcmmuse.shared.storage;

/**
 * 이미지 저장 추상화 (계약 원칙 Q8) — 데모=Local, 배포=S3. 계약(절대 URL 필드)은 불변.
 *
 * <p>DB에는 key가 아니라 {@link #resolveUrl(String)} 결과(절대 URL)를 저장한다 — 프론트가 {@code <img src>}로 바로 쓴다.
 */
public interface StorageService {

  /**
   * 바이트를 저장하고 key를 반환한다.
   *
   * @param prefix 용도별 디렉토리 (예: "scan", "closet", "looks")
   * @param extension 확장자 (예: "jpg", "png")
   */
  String store(byte[] data, String prefix, String extension);

  /** key → 프론트가 바로 쓰는 절대 URL */
  String resolveUrl(String key);

  /** 저장된 이미지 로드 — 화보 합성 입력 등 서버 내부 소비용 */
  byte[] load(String key);

  /** 절대 URL → key. 이 저장소가 서빙하는 URL이 아니면 null (외부 URL 판별용). */
  String keyOf(String url);
}
