package com.mutsapifa.mcmmuse.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 찜 (계약 §5-3~5-5) — (user, product) 유니크, 멱등 add/remove는 서비스에서 처리. BC 참조는 id로만. */
@Entity
@Table(
    name = "wishlist_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mcm_product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WishlistItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "mcm_product_id", nullable = false)
  private Long mcmProductId;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public WishlistItem(Long userId, Long mcmProductId) {
    this.userId = userId;
    this.mcmProductId = mcmProductId;
  }
}
