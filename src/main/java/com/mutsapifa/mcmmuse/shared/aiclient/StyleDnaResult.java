package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import java.util.List;

/** 스타일 DNA 결과 (계약 §4-1 응답 모양과 1:1). */
public record StyleDnaResult(
    String summary,
    List<Color> dominantColors,
    List<ItemMood> dominantMoods,
    List<String> keywords) {}
