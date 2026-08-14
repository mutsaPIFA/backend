package com.mutsapifa.mcmmuse.styling.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 저장된 룩(택1) — 유일하게 영속되는 스타일링 산출물 (DNA·추천·코디후보는 transient, 결정 Q9).
 *
 * <p>옷장 아이템 참조는 관계 테이블 {@code look_closet_items}(FK는 V1 스키마가 보장). 아이템이 소프트 삭제돼도 룩 조회에는 계속 나온다(과거 기록
 * 보존). MCM은 코디당 정확히 1개(결정 D5).
 *
 * <p>{@code generatedImageUrl}은 <b>비동기</b> 생성(결정 D2) — 저장 직후엔 null, 백그라운드 완료 시 채워진다. 프론트는 {@code
 * GET /looks/{id}} 폴링.
 */
@Entity
@Table(name = "looks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Look {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 미전송 시 서비스가 오늘 날짜로 채운다 (결정 D9) */
  @Column(name = "worn_date", nullable = false)
  private LocalDate wornDate;

  @Column(name = "mood_id", nullable = false)
  private Long moodId;

  @Column(name = "mcm_product_id", nullable = false)
  private Long mcmProductId;

  /** 코디 컨셉명 (영어 2~3단어, LLM 작명) — 룰베이스 폴백 후보였으면 null */
  @Column(length = 60)
  private String concept;

  @Column private String reason;

  /** 사용자 소감 — "이 코디 어땠어요?" 자유 텍스트 (AI reason과 별개) */
  @Column(columnDefinition = "text")
  private String note;

  @Column(name = "generated_image_url")
  private String generatedImageUrl;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "look_closet_items", joinColumns = @JoinColumn(name = "look_id"))
  @Column(name = "closet_item_id", nullable = false)
  private List<Long> closetItemIds = new ArrayList<>();

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public Look(
      Long userId,
      LocalDate wornDate,
      Long moodId,
      Long mcmProductId,
      String concept,
      String note,
      String reason,
      List<Long> closetItemIds) {
    this.userId = userId;
    this.wornDate = wornDate;
    this.moodId = moodId;
    this.mcmProductId = mcmProductId;
    this.concept = concept;
    this.note = note;
    this.reason = reason;
    this.closetItemIds = new ArrayList<>(closetItemIds);
  }

  /** 비동기 이미지 생성 완료 훅 */
  public void assignGeneratedImage(String url) {
    this.generatedImageUrl = url;
  }

  public boolean isOwnedBy(Long userId) {
    return this.userId.equals(userId);
  }
}
