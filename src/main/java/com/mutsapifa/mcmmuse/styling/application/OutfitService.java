package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiClosetItem;
import com.mutsapifa.mcmmuse.shared.aiclient.AiProduct;
import com.mutsapifa.mcmmuse.shared.aiclient.OutfitComposer;
import com.mutsapifa.mcmmuse.shared.aiclient.OutfitPick;
import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import com.mutsapifa.mcmmuse.styling.application.dto.OutfitResult;
import com.mutsapifa.mcmmuse.styling.domain.Mood;
import com.mutsapifa.mcmmuse.styling.domain.exception.MoodNotFoundException;
import com.mutsapifa.mcmmuse.styling.domain.exception.NoMcmInClosetException;
import com.mutsapifa.mcmmuse.styling.infrastructure.MoodRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계약 §4-4 — 코디 후보 생성 (미저장).
 *
 * <p>MCM 재료 = 옷장 {@code source=MCM} 중 {@code mcmProductId}가 있는 것(카탈로그 매칭분). 직접 촬영 MCM(N1: 매칭 안 함)은
 * 응답의 {@code mcmProduct{id,...}}를 조립할 수 없어 제외한다. {@code seedMcmProductId}가 오면 그 제품을 고정(제품상세 큐레이팅) —
 * 옷장 보유 여부와 무관.
 */
@Service
@Transactional(readOnly = true)
public class OutfitService {

  private final ClosetItemRepository closetItemRepository;
  private final McmProductRepository mcmProductRepository;
  private final MoodRepository moodRepository;
  private final OutfitComposer outfitComposer;

  public OutfitService(
      ClosetItemRepository closetItemRepository,
      McmProductRepository mcmProductRepository,
      MoodRepository moodRepository,
      OutfitComposer outfitComposer) {
    this.closetItemRepository = closetItemRepository;
    this.mcmProductRepository = mcmProductRepository;
    this.moodRepository = moodRepository;
    this.outfitComposer = outfitComposer;
  }

  public List<OutfitResult> compose(Long userId, Long moodId, Long seedMcmProductId) {
    Mood mood = moodRepository.findById(moodId).orElseThrow(MoodNotFoundException::new);

    List<ClosetItem> activeItems =
        closetItemRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    Map<Long, ClosetItem> itemById =
        activeItems.stream().collect(Collectors.toMap(ClosetItem::getId, Function.identity()));

    List<McmProduct> mcmCandidates = resolveMcmCandidates(activeItems, seedMcmProductId);
    if (mcmCandidates.isEmpty()) {
      throw new NoMcmInClosetException();
    }
    Map<Long, McmProduct> productById =
        mcmCandidates.stream().collect(Collectors.toMap(McmProduct::getId, Function.identity()));

    List<AiClosetItem> ownItems =
        activeItems.stream()
            .filter(it -> it.getSource() == Source.OWN)
            .map(
                it ->
                    new AiClosetItem(
                        it.getId(),
                        it.getCategory(),
                        it.getColor(),
                        it.getMaterial(),
                        it.getMood()))
            .toList();
    List<AiProduct> candidates =
        mcmCandidates.stream()
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

    // id 재검증: 실재하는 아이템·제품으로만 응답 조립 (환각 차단)
    List<OutfitResult> validated =
        outfitComposer.compose(mood.getLabel(), ownItems, candidates).stream()
            .map(pick -> toResult(pick, mood, itemById, productById))
            .filter(Objects::nonNull)
            .toList();
    return selectDistinctLooks(validated, maxLooks(activeItems, mcmCandidates.size()));
  }

  /**
   * 후보 수 정책 — 재고가 적으면 억지로 3개를 만들다 서로 겹친다. 상·하의 짝 재고(min)가 다양성의 상한이므로:
   * 기본 1개 / 짝 재고 3 이상 → 2개 / 짝 재고 5 이상 + MCM 재료 2종 이상 → 3개.
   * 상단 카운트에는 아우터도 포함(레이어링으로 다양성을 만든다), 원피스는 상·하의를 모두 대신한다.
   */
  private int maxLooks(List<ClosetItem> activeItems, int mcmCandidateCount) {
    long tops =
        activeItems.stream()
            .filter(
                it ->
                    it.getCategory() == Category.상의
                        || it.getCategory() == Category.아우터
                        || it.getCategory() == Category.원피스)
            .count();
    long bottoms =
        activeItems.stream()
            .filter(it -> it.getCategory() == Category.하의 || it.getCategory() == Category.원피스)
            .count();
    long pairStock = Math.min(tops, bottoms);
    if (pairStock >= 5 && mcmCandidateCount >= 2) {
      return 3;
    }
    return pairStock >= 3 ? 2 : 1;
  }

  /** 중복 제약 — 어떤 두 후보도 같은 아이템·제품을 2개 이상 공유하지 않게 순서대로 선별한다 (ai 응답은 추천 순). */
  private List<OutfitResult> selectDistinctLooks(List<OutfitResult> validated, int maxLooks) {
    List<Set<String>> keptKeys = new ArrayList<>();
    List<OutfitResult> selected = new ArrayList<>();
    for (OutfitResult look : validated) {
      Set<String> keys = new HashSet<>();
      look.closetItems().forEach(item -> keys.add("i" + item.id()));
      keys.add("m" + look.mcmProduct().id());
      boolean overlaps =
          keptKeys.stream().anyMatch(kept -> keys.stream().filter(kept::contains).count() >= 2);
      if (overlaps) {
        continue;
      }
      keptKeys.add(keys);
      selected.add(look);
      if (selected.size() >= maxLooks) {
        break;
      }
    }
    return selected;
  }

  private List<McmProduct> resolveMcmCandidates(
      List<ClosetItem> activeItems, Long seedMcmProductId) {
    if (seedMcmProductId != null) {
      return mcmProductRepository.findById(seedMcmProductId).map(List::of).orElse(List.of());
    }
    List<Long> ownedProductIds =
        activeItems.stream()
            .filter(it -> it.getSource() == Source.MCM && it.getMcmProductId() != null)
            .map(ClosetItem::getMcmProductId)
            .distinct()
            .toList();
    return mcmProductRepository.findAllById(ownedProductIds);
  }

  private OutfitResult toResult(
      OutfitPick pick,
      Mood mood,
      Map<Long, ClosetItem> itemById,
      Map<Long, McmProduct> productById) {
    McmProduct product = productById.get(pick.mcmProductId());
    if (product == null) {
      return null;
    }
    List<OutfitResult.ItemSummary> items =
        pick.closetItemIds().stream()
            .map(itemById::get)
            .filter(Objects::nonNull)
            .map(
                it ->
                    new OutfitResult.ItemSummary(
                        it.getId(), it.getCutoutUrl(), it.getCategory(), it.getColor(), it.getMaterial()))
            .toList();
    return new OutfitResult(
        mood.getId(),
        mood.occasionLabel(),
        pick.concept(),
        null, // 화보는 트랜잭션 밖에서 붙인다 (OutfitImageService — 생성 20초간 DB 커넥션 점유 방지)
        items,
        new OutfitResult.McmSummary(
            product.getId(), product.getImageUrl(), product.getCutoutUrl(), product.getName()),
        pick.reason());
  }
}
