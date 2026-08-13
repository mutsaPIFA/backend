package com.mutsapifa.mcmmuse.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 항상 소문자로 정규화해 저장한다 (DB UNIQUE와 짝). */
  @Column(nullable = false, unique = true)
  private String email;

  /** BCrypt 해시 */
  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String nickname;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public User(String email, String password, String nickname) {
    this.email = email;
    this.password = password;
    this.nickname = nickname;
  }
}
