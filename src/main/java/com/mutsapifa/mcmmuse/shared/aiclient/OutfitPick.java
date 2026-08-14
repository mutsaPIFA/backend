package com.mutsapifa.mcmmuse.shared.aiclient;

import java.util.List;

/** 코디 후보 1건 — id 조합 + 컨셉명(영어, 룰베이스 폴백 시 null) + 이유. 응답 조립은 호출부(styling) 몫. */
public record OutfitPick(List<Long> closetItemIds, Long mcmProductId, String concept, String reason) {}
