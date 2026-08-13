package com.mutsapifa.mcmmuse.closet.application.dto;

import com.mutsapifa.mcmmuse.shared.aiclient.ScanTags;

/** 스캔 결과 (미저장) — 프론트가 들고 있다가 POST /closet-items 로 등록한다. */
public record ScanResult(String originalUrl, String cutoutUrl, ScanTags tags) {}
