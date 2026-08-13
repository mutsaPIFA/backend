package com.mutsapifa.mcmmuse.closet.presentation.dto;

import com.mutsapifa.mcmmuse.closet.application.dto.ScanResult;
import com.mutsapifa.mcmmuse.shared.aiclient.ScanTags;

/** 계약 §3-1 — 200 {originalUrl, cutoutUrl, tags{category,color,material,mood}} */
public record ScanResponse(String originalUrl, String cutoutUrl, ScanTags tags) {

  public static ScanResponse from(ScanResult result) {
    return new ScanResponse(result.originalUrl(), result.cutoutUrl(), result.tags());
  }
}
