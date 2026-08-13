package com.mutsapifa.mcmmuse.shared.aiclient;

/**
 * 스캔 표준화 포트 — 대충 찍은 옷 사진을 쇼핑몰 상품컷으로 재생성 (스캔 품질의 핵심).
 *
 * <p>구현: Gemini 이미지 생성 HTTP(ai 서비스 /vision/standardize) 또는 mock(원본 통과 = rembg 단독 폴백과 동일).
 */
public interface ImageStandardizer {

  byte[] standardize(byte[] image);
}
