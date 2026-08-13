package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/** 코디 후보 1건 — id 조합 + 이유. 응답 조립(cutoutUrl·occasionLabel)은 호출부(styling) 몫. */
public record OutfitPick(List<Long> closetItemIds, Long mcmProductId, String reason) {}
