package com.mutsapifa.mcmmuse.shared.aiclient;

/** 비전 태깅 포트 — 구현: Gemini HTTP(ai 서비스 /vision/tag) 또는 mock(해시 기반). */
public interface VisionTagger {

  ScanTags tag(byte[] image);
}
