package com.mutsapifa.mcmmuse.closet.domain;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.ItemMood;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
import com.mutsapifa.mcmmuse.shared.vocab.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 옷장 아이템 — <b>소프트 삭제</b>({@code deletedAt}). row는 지우지 않는다: 저장된 룩(look_closet_items FK)이 참조하고, 과거 코디
 * 기록·콜라주가 살아 있어야 한다. 활성 조회는 반드시 {@code deletedAt IS NULL} (DB-컨벤션 참조).
 *
 * <p>BC 간 참조는 id 값으로만: {@code userId}(auth), {@code mcmProductId}(catalog).
 */
@Entity
@Table(name = "closet_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ClosetItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 사용자 지정 명칭 (계약 §3-6) — null이면 프론트가 태그 조합으로 표시명을 만든다 */
  @Column(name = "custom_name", length = 30)
  private String customName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Color color;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Material material;

  /** 아이템 무드 태그 — 큐레이터 Mood(styling)와 별개 */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ItemMood mood;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  @Column(name = "cutout_url")
  private String cutoutUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Source source;

  /** source=MCM 카탈로그 담기일 때만 값이 있다 (촬영 등록 MCM은 null — 결정 N1) */
  @Column(name = "mcm_product_id")
  private Long mcmProductId;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  /** 스캔 결과 등록 (source=OWN 또는 보유 MCM) */
  public ClosetItem(
      Long userId,
      Category category,
      Color color,
      Material material,
      ItemMood mood,
      String imageUrl,
      String cutoutUrl,
      Source source) {
    this.userId = userId;
    this.category = category;
    this.color = color;
    this.material = material;
    this.mood = mood;
    this.imageUrl = imageUrl;
    this.cutoutUrl = cutoutUrl;
    this.source = source;
  }

  /** 게스트 시드(§1-6) — 템플릿 사용자의 아이템을 새 사용자 소유로 복사. 이미지 URL은 공유(파일 복사 없음). */
  public static ClosetItem copyOf(Long newUserId, ClosetItem source) {
    ClosetItem item =
        new ClosetItem(
            newUserId,
            source.category,
            source.color,
            source.material,
            source.mood,
            source.imageUrl,
            source.cutoutUrl,
            source.source);
    item.customName = source.customName;
    item.mcmProductId = source.mcmProductId;
    return item;
  }

  /** 카탈로그 MCM 담기 — 태그·이미지는 제품에서 복사 */
  public static ClosetItem fromCatalog(
      Long userId,
      Long mcmProductId,
      Category category,
      Color color,
      Material material,
      ItemMood mood,
      String imageUrl,
      String cutoutUrl) {
    ClosetItem item =
        new ClosetItem(userId, category, color, material, mood, imageUrl, cutoutUrl, Source.MCM);
    item.mcmProductId = mcmProductId;
    return item;
  }

  /**
   * 부분 수정 (계약 §3-6) — null 인자는 유지. 명칭은 빈 문자열이 오면 제거(태그 조합 표시로 복귀).
   */
  public void edit(String name, Category category, Color color, Material material, ItemMood mood) {
    if (name != null) {
      this.customName = name.isBlank() ? null : name.trim();
    }
    if (category != null) {
      this.category = category;
    }
    if (color != null) {
      this.color = color;
    }
    if (material != null) {
      this.material = material;
    }
    if (mood != null) {
      this.mood = mood;
    }
  }

  /** 소프트 삭제 — 옷장 목록에서만 사라진다 */
  public void delete(Instant now) {
    this.deletedAt = now;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }
}
