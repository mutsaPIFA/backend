package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiClosetItem;
import com.mutsapifa.mcmmuse.shared.aiclient.AiProduct;
import com.mutsapifa.mcmmuse.shared.aiclient.RecommendationPick;
import com.mutsapifa.mcmmuse.shared.aiclient.Recommender;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import com.mutsapifa.mcmmuse.styling.application.dto.RecommendationResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계약 §4-2 — MCM 추천. 미저장(transient).
 *
 * <p>계약 원칙: AI가 제안한 id는 신뢰하지 않는다 — <b>DB에 실재하는 active 상품만</b> 응답에 싣는다(환각 차단).
 *
 * <p>후보는 LLM에 넘기기 전에 옷장 태그 기준으로 좁힌다. 카탈로그 전량을 넘기면 콜당 프롬프트가 상품 수에 비례해 커지고(589건 기준 5만자), 카탈로그가 늘수록
 * 그대로 한도에 부딪힌다.
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

  /** LLM에 넘길 후보 상한 — 고르는 역할이지 전수 스캔이 아니다. */
  private static final int CANDIDATE_LIMIT = 60;

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
        narrow(List.copyOf(catalog.values()), items).stream()
            .map(
                p ->
                    new AiProduct(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getColor(),
                        p.getMaterial(),
                        p.getMood(),
                        p.getStyleNote()))
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

  /**
   * 옷장 태그와의 겹침 점수로 후보를 {@value #CANDIDATE_LIMIT}건까지 좁힌다.
   *
   * <p>점수만 쓰면 한 카테고리(가방·악세서리가 카탈로그의 2/3)로 쏠려 코디가 성립하지 않으므로, 카테고리별로 먼저 고르게 뽑고 남는 자리를 점수순으로 채운다. "취향을
   * 한 단계 확장하는 제품"은 이 카테고리 배분에서 들어오는 저점수 후보가 맡는다.
   */
  private List<McmProduct> narrow(List<McmProduct> catalog, List<AiClosetItem> items) {
    if (catalog.size() <= CANDIDATE_LIMIT) {
      return catalog;
    }
    Set<ItemMood> moods =
        items.stream().map(AiClosetItem::mood).filter(Objects::nonNull).collect(Collectors.toSet());
    Set<Color> colors =
        items.stream()
            .map(AiClosetItem::color)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Set<Material> materials =
        items.stream()
            .map(AiClosetItem::material)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    // id 2차 정렬 — 같은 점수면 매 호출 같은 후보가 나오도록(재현 가능한 추천)
    Comparator<McmProduct> ranked =
        Comparator.comparingInt((McmProduct p) -> -score(p, moods, colors, materials))
            .thenComparing(McmProduct::getId);

    Map<Category, List<McmProduct>> byCategory =
        catalog.stream().collect(Collectors.groupingBy(McmProduct::getCategory));
    int perCategory = Math.max(1, CANDIDATE_LIMIT / byCategory.size());

    List<McmProduct> picked = new ArrayList<>();
    for (List<McmProduct> group : byCategory.values()) {
      group.stream().sorted(ranked).limit(perCategory).forEach(picked::add);
    }
    if (picked.size() < CANDIDATE_LIMIT) {
      Set<Long> taken = picked.stream().map(McmProduct::getId).collect(Collectors.toSet());
      catalog.stream()
          .filter(p -> !taken.contains(p.getId()))
          .sorted(ranked)
          .limit(CANDIDATE_LIMIT - (long) picked.size())
          .forEach(picked::add);
    }
    return picked;
  }

  private static int score(
      McmProduct p, Set<ItemMood> moods, Set<Color> colors, Set<Material> materials) {
    int score = 0;
    if (p.getMood() != null && moods.contains(p.getMood())) {
      score += 3;
    }
    if (colors.contains(p.getColor())) {
      score += 2;
    }
    if (materials.contains(p.getMaterial())) {
      score += 1;
    }
    return score;
  }
}
