package com.mutsapifa.mcmmuse.styling.infrastructure;

import com.mutsapifa.mcmmuse.styling.domain.Mood;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodRepository extends JpaRepository<Mood, Long> {

  /** GET /moods — 시드 6개, id 순 고정 */
  List<Mood> findAllByOrderByIdAsc();
}
