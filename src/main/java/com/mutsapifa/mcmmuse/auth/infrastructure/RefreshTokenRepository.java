package com.mutsapifa.mcmmuse.auth.infrastructure;

import com.mutsapifa.mcmmuse.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  void deleteByTokenHash(String tokenHash);

  void deleteByUserId(Long userId);
}
