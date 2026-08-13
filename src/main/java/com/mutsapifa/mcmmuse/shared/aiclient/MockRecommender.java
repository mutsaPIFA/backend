package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 룰베이스 mock — Gemini 없이도 데모 가능한 수준의 추천을 만든다.
 *
 * <p>결정론적(같은 입력 → 같은 결과). Gemini 전환 후에도 폴백으로 유지된다.
 */
public class MockRecommender implements Recommender {

  private static final Map<ItemMood, List<String>> MOOD_KEYWORDS =
      Map.of(
          ItemMood.미니멀, List.of("차분한", "베이직", "정제된"),
          ItemMood.캐주얼, List.of("편안한", "데일리", "활동적인"),
          ItemMood.클래식, List.of("포멀", "단정한", "타임리스"),
          ItemMood.스트릿, List.of("자유로운", "볼드한", "개성있는"),
          ItemMood.페미닌, List.of("부드러운", "우아한", "산뜻한"),
          ItemMood.럭셔리, List.of("고급스러운", "시그니처", "아이코닉"));

  @Override
  public StyleDnaResult styleDna(List<AiClosetItem> items) {
    List<Color> colors = topBy(items, AiClosetItem::color, 2);
    List<ItemMood> moods = topBy(items, AiClosetItem::mood, 2);
    String summary =
        colors.get(0)
            + "·"
            + moods.get(0)
            + " 중심의 "
            + (moods.size() > 1 ? moods.get(1) : moods.get(0))
            + " 무드";
    List<String> keywords = MOOD_KEYWORDS.getOrDefault(moods.get(0), List.of("베이직"));
    return new StyleDnaResult(summary, colors, moods, keywords);
  }

  @Override
  public List<RecommendationPick> recommend(List<AiClosetItem> items, List<AiProduct> candidates) {
    List<Color> dominantColors = topBy(items, AiClosetItem::color, 2);
    return candidates.stream()
        .sorted(
            Comparator.comparingInt((AiProduct p) -> -score(p, items, dominantColors))
                .thenComparing(AiProduct::id))
        .limit(5)
        .map(p -> new RecommendationPick(p.id(), reason(p, dominantColors), pairs(p, items)))
        .toList();
  }

  /** 색 일치 +3, 소재 일치 +1, 가방 +2(MCM 시그니처 우선) */
  private int score(AiProduct p, List<AiClosetItem> items, List<Color> dominantColors) {
    int s = 0;
    if (dominantColors.contains(p.color())) s += 3;
    if (items.stream().anyMatch(i -> i.material() == p.material())) s += 1;
    if (p.category() == Category.가방) s += 2;
    return s;
  }

  private String reason(AiProduct p, List<Color> dominantColors) {
    Color base = dominantColors.get(0);
    if (dominantColors.contains(p.color())) {
      return base + " 톤 옷장과 자연스럽게 이어지는 " + p.name();
    }
    return base + " 중심 옷장에 포인트가 되는 " + p.color() + " " + p.name();
  }

  /** 같은 색 계열 아이템 최대 2개를 매치로 제시 */
  private List<Long> pairs(AiProduct p, List<AiClosetItem> items) {
    List<Long> matched =
        items.stream().filter(i -> i.color() == p.color()).map(AiClosetItem::id).limit(2).toList();
    if (!matched.isEmpty()) {
      return matched;
    }
    return items.stream().map(AiClosetItem::id).limit(2).toList();
  }

  private static <T, K> List<K> topBy(List<T> items, Function<T, K> key, int n) {
    return items.stream()
        .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(Map.Entry.<K, Long>comparingByValue().reversed())
        .limit(n)
        .map(Map.Entry::getKey)
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
