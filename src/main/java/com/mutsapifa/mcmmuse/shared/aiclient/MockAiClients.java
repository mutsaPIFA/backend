package com.mutsapifa.mcmmuse.shared.aiclient;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import java.util.Arrays;

/**
 * Mock 구현 모음 — AI 서비스·Gemini 키 없이도 전체 플로우가 동작하게 한다.
 *
 * <p>태깅 mock은 이미지 바이트 해시 기반 <b>결정론적 다양화</b>: 같은 사진은 항상 같은 태그(재스캔 UX·테스트 일관성), 다른 사진은 다른 태그(데모
 * 자연스러움). 어차피 사용자가 화면 3에서 수정 가능.
 */
public final class MockAiClients {

  private MockAiClients() {}

  /** 누끼 mock — 원본 그대로 (배경 있는 콜라주가 되지만 동작은 함) */
  public static class MockBackgroundRemover implements BackgroundRemover {
    @Override
    public byte[] remove(byte[] image) {
      return image;
    }
  }

  /** 표준화 mock — 원본 통과 (rembg 단독 폴백과 동일한 품질) */
  public static class MockImageStandardizer implements ImageStandardizer {
    @Override
    public byte[] standardize(byte[] image) {
      return image;
    }
  }

  /** 태깅 mock — 해시 기반 결정론적 선택. 옷 스캔 맥락이라 가방·악세서리는 후보에서 제외 */
  public static class MockVisionTagger implements VisionTagger {

    private static final Category[] CATEGORIES = {
      Category.상의, Category.하의, Category.아우터, Category.원피스, Category.신발
    };
    private static final Color[] COLORS = {
      Color.블랙, Color.화이트, Color.네이비, Color.그레이, Color.베이지, Color.브라운
    };
    private static final Material[] MATERIALS = {Material.면, Material.니트, Material.데님, Material.울};

    @Override
    public ScanTags tag(byte[] image) {
      int h = Math.abs(Arrays.hashCode(image));
      return new ScanTags(
          CATEGORIES[h % CATEGORIES.length],
          COLORS[(h / 7) % COLORS.length],
          MATERIALS[(h / 31) % MATERIALS.length],
          ItemMood.values()[(h / 131) % ItemMood.values().length]);
    }
  }
}
