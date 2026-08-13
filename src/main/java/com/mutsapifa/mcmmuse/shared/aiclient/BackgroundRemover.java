package com.mutsapifa.mcmmuse.shared.aiclient;

/** 누끼(배경 제거) 포트 — 구현: rembg HTTP(ai 서비스 /cutout) 또는 mock(원본 통과). */
public interface BackgroundRemover {

  /** 이미지 → 투명 배경 PNG 바이트 */
  byte[] remove(byte[] image);
}
