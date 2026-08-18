package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;

/**
 * AI 포트 입력용 MCM 상품 후보 요약.
 *
 * <p>{@code styleNote}는 한 줄 요약(평균 43자)이다. 상세 설명(평균 144자)은 후보 수만큼 곱해져 프롬프트가 감당 못 하므로 넣지 않는다 — 상세 설명은
 * 후보가 1건인 제품상세 큐레이팅 경로에서만 쓴다.
 */
public record AiProduct(
    Long id,
    String name,
    Category category,
    Color color,
    Material material,
    ItemMood mood,
    String styleNote) {}
