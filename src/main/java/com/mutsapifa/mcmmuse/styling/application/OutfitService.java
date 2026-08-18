package com.mutsapifa.mcmmuse.styling.application;

import com.mutsapifa.mcmmuse.catalog.domain.McmProduct;
import com.mutsapifa.mcmmuse.catalog.infrastructure.McmProductRepository;
import com.mutsapifa.mcmmuse.closet.domain.ClosetItem;
import com.mutsapifa.mcmmuse.closet.infrastructure.ClosetItemRepository;
import com.mutsapifa.mcmmuse.shared.aiclient.AiClosetItem;
import com.mutsapifa.mcmmuse.shared.aiclient.AiProduct;
import com.mutsapifa.mcmmuse.shared.aiclient.OutfitComposer;
import com.mutsapifa.mcmmuse.shared.aiclient.OutfitPick;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import com.mutsapifa.mcmmuse.styling.application.dto.OutfitResult;
import com.mutsapifa.mcmmuse.styling.domain.Mood;
import com.mutsapifa.mcmmuse.styling.domain.exception.MoodNotFoundException;
import com.mutsapifa.mcmmuse.styling.domain.exception.NoMcmInClosetException;
import com.mutsapifa.mcmmuse.styling.infrastructure.MoodRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    return outfitComposer.compose(mood.getLabel(), ownItems, candidates).stream()
        .map(pick -> toResult(pick, mood, itemById, productById))
        .filter(Objects::nonNull)
        .limit(3)
        .toList();
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
                it -> new OutfitResult.ItemSummary(it.getId(), it.getCutoutUrl(), it.getCategory()))
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
