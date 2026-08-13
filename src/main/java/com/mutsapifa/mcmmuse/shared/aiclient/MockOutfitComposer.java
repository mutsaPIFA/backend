package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 룰베이스 코디 조합 mock — 상의+하의(+아우터) / 원피스(+신발) 골격에 MCM 1개씩.
 *
 * <p>결정론적. 골격×MCM을 로테이션해 서로 다른 후보 최대 3개.
 */
public class MockOutfitComposer implements OutfitComposer {

  @Override
  public List<OutfitPick> compose(
      String moodLabel, List<AiClosetItem> ownItems, List<AiProduct> mcmCandidates) {
    if (mcmCandidates.isEmpty()) {
      return List.of();
    }

    List<List<AiClosetItem>> bases = buildBases(ownItems);
    if (bases.isEmpty()) {
      // TODO(추후 수정): OWN 옷 0벌일 때 MCM 단독 후보 1개 반환은 임시 동작.
      // 빈 옷장 UX(옷 등록 유도 등)가 정해지면 재설계.
      AiProduct mcm = mcmCandidates.get(0);
      return List.of(new OutfitPick(List.of(), mcm.id(), moodLabel + "의 시작점이 되는 " + mcm.name()));
    }

    int count = Math.min(3, Math.max(bases.size(), Math.min(mcmCandidates.size(), 3)));
    List<OutfitPick> picks = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      List<AiClosetItem> base = bases.get(i % bases.size());
      AiProduct mcm = mcmCandidates.get(i % mcmCandidates.size());
      picks.add(
          new OutfitPick(
              base.stream().map(AiClosetItem::id).toList(),
              mcm.id(),
              moodLabel + "에 어울리는 " + base.get(0).color() + " 조합, " + mcm.name() + "(으)로 포인트"));
    }
    return picks.stream().distinct().toList();
  }

  /** 코디 골격: 상의×하의 짝 최대 2개(+아우터), 원피스(+신발) 1개, 그것도 없으면 아무 아이템 1~2개 */
  private List<List<AiClosetItem>> buildBases(List<AiClosetItem> ownItems) {
    Map<Category, List<AiClosetItem>> byCategory =
        ownItems.stream().collect(Collectors.groupingBy(AiClosetItem::category));
    List<AiClosetItem> tops = byCategory.getOrDefault(Category.상의, List.of());
    List<AiClosetItem> bottoms = byCategory.getOrDefault(Category.하의, List.of());
    List<AiClosetItem> dresses = byCategory.getOrDefault(Category.원피스, List.of());
    List<AiClosetItem> outers = byCategory.getOrDefault(Category.아우터, List.of());
    List<AiClosetItem> shoes = byCategory.getOrDefault(Category.신발, List.of());

    List<List<AiClosetItem>> bases = new ArrayList<>();
    for (int i = 0; i < Math.min(2, Math.min(tops.size(), bottoms.size())); i++) {
      List<AiClosetItem> base =
          new ArrayList<>(List.of(tops.get(i), bottoms.get(i % bottoms.size())));
      if (i < outers.size()) {
        base.add(outers.get(i));
      }
      bases.add(base);
    }
    if (!dresses.isEmpty()) {
      List<AiClosetItem> base = new ArrayList<>(List.of(dresses.get(0)));
      if (!shoes.isEmpty()) {
        base.add(shoes.get(0));
      }
      bases.add(base);
    }
    if (bases.isEmpty() && !ownItems.isEmpty()) {
      bases.add(ownItems.stream().limit(2).toList());
    }
    return bases;
  }
}
