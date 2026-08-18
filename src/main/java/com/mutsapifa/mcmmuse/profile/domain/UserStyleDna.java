package com.mutsapifa.mcmmuse.profile.domain;

import com.mutsapifa.mcmmuse.catalog.domain.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 최근 스타일 DNA 스냅샷 (계약 §1-5 styleDna) — 사용자당 1행, §4-1 성공 시 통째로 갱신. 목록 필드는 '|' 구분 text (ERD 컨벤션 —
 * URL·태그 어휘에 '|'가 없다).
 */
@Entity
@Table(name = "user_style_dna")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStyleDna {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @Column(nullable = false, columnDefinition = "text")
  private String summary;

  @Convert(converter = StringListConverter.class)
  @Column(name = "dominant_colors", columnDefinition = "text")
  private List<String> dominantColors;

  @Convert(converter = StringListConverter.class)
  @Column(name = "dominant_moods", columnDefinition = "text")
  private List<String> dominantMoods;

  @Convert(converter = StringListConverter.class)
  @Column(columnDefinition = "text")
  private List<String> keywords;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UserStyleDna(
      Long userId,
      String summary,
      List<String> dominantColors,
      List<String> dominantMoods,
      List<String> keywords,
      Instant updatedAt) {
    this.userId = userId;
    this.summary = summary;
    this.dominantColors = dominantColors;
    this.dominantMoods = dominantMoods;
    this.keywords = keywords;
    this.updatedAt = updatedAt;
  }

  /** §4-1이 성공할 때마다 최신 값으로 교체 */
  public void refresh(
      String summary,
      List<String> dominantColors,
      List<String> dominantMoods,
      List<String> keywords,
      Instant updatedAt) {
    this.summary = summary;
    this.dominantColors = dominantColors;
    this.dominantMoods = dominantMoods;
    this.keywords = keywords;
    this.updatedAt = updatedAt;
  }
}
