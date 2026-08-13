package com.mutsapifa.mcmmuse.styling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 큐레이터 무드 — 고정 시드 6개 (V2, 계약상 moodId 1~6). 코드에서 생성하지 않는다.
 *
 * <p>⚠️ 옷장 아이템 태그 {@code ItemMood}(shared/vocab)와 별개.
 *
 * <p>라벨은 Figma 무드 카드와 정렬 확정 대기 — 바뀌면 V3 마이그레이션으로 UPDATE. Look은 {@code occasionLabel}을 저장하지 않고 이
 * 엔티티를 조인해 조립한다(라벨 변경이 과거 기록에도 반영).
 */
@Entity
@Table(name = "moods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mood {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** "저녁 약속" */
  @Column(nullable = false, length = 30)
  private String label;

  /** "DINNER DATE" */
  @Column(name = "label_en", nullable = false, length = 30)
  private String labelEn;

  /** "dinner" — 프론트 아이콘 키 */
  @Column(name = "icon_key", nullable = false, length = 20)
  private String iconKey;

  /** 계약 응답 occasionLabel — "저녁 약속 / DINNER DATE" */
  public String occasionLabel() {
    return label + " / " + labelEn;
  }
}
