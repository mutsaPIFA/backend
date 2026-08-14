package com.mutsapifa.mcmmuse.styling.infrastructure;

import com.mutsapifa.mcmmuse.styling.domain.Look;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LookRepository extends JpaRepository<Look, Long> {

  /** GET /looks/{id} — 소유권 검사 포함 단건 (이미지 생성 폴링) */
  Optional<Look> findByIdAndUserId(Long id, Long userId);

  /** GET /looks — 전체 (month 미지정). 하루 여러 룩 허용 — 같은 날짜는 최근 저장순 */
  List<Look> findByUserIdOrderByWornDateDescCreatedAtDesc(Long userId);

  /** GET /looks?month= — yyyy-MM 필터. 정렬 규칙 동일 */
  List<Look> findByUserIdAndWornDateBetweenOrderByWornDateDescCreatedAtDesc(
      Long userId, LocalDate start, LocalDate end);
}
