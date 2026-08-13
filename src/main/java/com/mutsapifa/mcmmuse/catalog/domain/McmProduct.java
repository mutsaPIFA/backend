package com.mutsapifa.mcmmuse.catalog.domain;

import com.mutsapifa.mcmmuse.shared.vocab.Category;
import com.mutsapifa.mcmmuse.shared.vocab.Color;
import com.mutsapifa.mcmmuse.shared.vocab.Material;
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
 * MCM 카탈로그 상품. 시드/피드 적재는 {@code sku} 기준 upsert — row 삭제 금지(옷장·룩이 id 참조), 피드 이탈 시 {@link
 * #deactivate()}.
 */
@Entity
@Table(name = "mcm_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class McmProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** MCM 공식몰 SKU — 시드 upsert 키 */
  @Column(nullable = false, unique = true, length = 40)
  private String sku;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Category category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Color color;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Material material;

  /** KRW 원 단위 */
  @Column(nullable = false)
  private Integer price;

  @Column(name = "image_url", nullable = false)
  private String imageUrl;

  /** 누끼 — 적재 시 rembg 생성, 실패 시 null (화면 16 콜라주용) */
  @Column(name = "cutout_url")
  private String cutoutUrl;

  /** 공식몰 PDP — 화면 6 "구매하기" 딥링크 */
  @Column(name = "product_url", nullable = false)
  private String productUrl;

  /** 피드에서 사라지면 false — 카탈로그 조회에서만 숨는다 */
  @Column(nullable = false)
  private boolean active = true;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public McmProduct(
      String sku,
      String name,
      Category category,
      Color color,
      Material material,
      Integer price,
      String imageUrl,
      String productUrl) {
    this.sku = sku;
    this.name = name;
    this.category = category;
    this.color = color;
    this.material = material;
    this.price = price;
    this.imageUrl = imageUrl;
    this.productUrl = productUrl;
  }

  /** 재적재(upsert) 시 갱신 — sku·cutoutUrl은 건드리지 않는다 */
  public void updateFrom(
      String name,
      Category category,
      Color color,
      Material material,
      Integer price,
      String imageUrl,
      String productUrl) {
    this.name = name;
    this.category = category;
    this.color = color;
    this.material = material;
    this.price = price;
    this.imageUrl = imageUrl;
    this.productUrl = productUrl;
    this.active = true;
  }

  public void assignCutout(String cutoutUrl) {
    this.cutoutUrl = cutoutUrl;
  }

  public void deactivate() {
    this.active = false;
  }
}
