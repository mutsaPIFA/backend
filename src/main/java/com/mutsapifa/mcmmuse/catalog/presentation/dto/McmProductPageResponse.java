package com.mutsapifa.mcmmuse.catalog.presentation.dto;

import com.mutsapifa.mcmmuse.catalog.application.McmProductService.PagedProducts;
import java.util.List;

/** 계약 §2-1 — page 파라미터 사용 시의 페이지 응답 봉투. */
public record McmProductPageResponse(
    List<McmProductResponse> items, int page, int size, long totalItems, int totalPages) {

  public static McmProductPageResponse from(PagedProducts paged) {
    return new McmProductPageResponse(
        paged.items().stream().map(McmProductResponse::from).toList(),
        paged.page(),
        paged.size(),
        paged.totalItems(),
        paged.totalPages());
  }
}
