package com.mutsapifa.mcmmuse.catalog.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.exception.BusinessException;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계약 §2 — 카탈로그 조회. 목록은 항상 active 필터. */
@Service
@Transactional(readOnly = true)
public class McmProductService {

  private final McmProductRepository mcmProductRepository;

  public McmProductService(McmProductRepository mcmProductRepository) {
    this.mcmProductRepository = mcmProductRepository;
  }

  /**
   * §2-1 — query(이름 부분일치)·category 필터. v1 프론트는 파라미터 미사용(전체 수신 후 클라 필터)이지만 계약대로 서버에도 구현. 146건 규모라
   * 인메모리 필터로 충분 — 데이터 증가 시 쿼리로 이관.
   */
  public List<McmProduct> list(String query, Category category) {
    return mcmProductRepository.findByActiveTrueOrderByIdAsc().stream()
        .filter(p -> category == null || p.getCategory() == category)
        .filter(
            p ->
                query == null
                    || query.isBlank()
                    || p.getName().toLowerCase().contains(query.toLowerCase()))
        .toList();
  }

  /** §2-2 — 상세. 옷장에 담긴 비활성 상품도 상세는 열려야 하므로 active 필터 없음. */
  public McmProduct get(Long id) {
    return mcmProductRepository
        .findById(id)
        .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "제품을 찾을 수 없습니다"));
  }
}
