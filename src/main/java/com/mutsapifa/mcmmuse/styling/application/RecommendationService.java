package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiClosetItem;
import com.mutsapifa.mcmmuse.shared.aiclient.AiProduct;
import com.mutsapifa.mcmmuse.shared.aiclient.RecommendationPick;
import com.mutsapifa.mcmmuse.shared.aiclient.Recommender;
import com.mutsapifa.mcmmuse.styling.application.dto.RecommendationResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계약 §4-2 — MCM 추천. 미저장(transient).
 *
 * <p>계약 원칙: AI가 제안한 id는 신뢰하지 않는다 — <b>DB에 실재하는 active 상품만</b> 응답에 싣는다(환각 차단).
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

  private final StylingQueryService stylingQueryService;
  private final McmProductRepository mcmProductRepository; // styling → catalog 허용 방향
  private final Recommender recommender;

  public RecommendationService(
      StylingQueryService stylingQueryService,
      McmProductRepository mcmProductRepository,
      Recommender recommender) {
    this.stylingQueryService = stylingQueryService;
    this.mcmProductRepository = mcmProductRepository;
    this.recommender = recommender;
  }

  public RecommendationResult recommend(Long userId, List<Long> closetItemIds) {
    List<AiClosetItem> items = stylingQueryService.loadOwnedItems(userId, closetItemIds);

    Map<Long, McmProduct> catalog =
        mcmProductRepository.findByActiveTrueOrderByIdAsc().stream()
            .collect(Collectors.toMap(McmProduct::getId, p -> p));
    List<AiProduct> candidates =
        catalog.values().stream()
            .map(
                p ->
                    new AiProduct(
                        p.getId(), p.getName(), p.getCategory(), p.getColor(), p.getMaterial()))
            .toList();

    // id 재검증: DB(active)에 실재하는 추천만 통과
    List<RecommendationResult.Item> verified =
        recommender.recommend(items, candidates).stream()
            .filter(pick -> catalog.containsKey(pick.productId()))
            .map(pick -> toItem(pick, catalog.get(pick.productId())))
            .toList();

    if (verified.isEmpty()) {
      return new RecommendationResult(null, List.of());
    }
    return new RecommendationResult(verified.get(0), verified.subList(1, verified.size()));
  }

  private RecommendationResult.Item toItem(RecommendationPick pick, McmProduct product) {
    return new RecommendationResult.Item(
        product.getId(),
        pick.reason(),
        pick.pairsWithItemIds(),
        false, // isExpansion — v1 항상 false (계약)
        new RecommendationResult.ProductSummary(
            product.getId(),
            product.getName(),
            product.getImageUrl(),
            product.getPrice(),
            product.getProductUrl()));
  }
}
