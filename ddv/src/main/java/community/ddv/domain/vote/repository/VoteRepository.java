package community.ddv.domain.vote.repository;

import community.ddv.domain.vote.entity.Vote;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

  // 투표가 진행중인지 확인 (시작일이 현재보다 먼저여야 하고, 마감일이 현재 이후여야 함
  Optional<Vote> findByStartDateBeforeAndEndDateAfter(LocalDateTime start, LocalDateTime end);

  // 이번주에 생성된 투표가 있는지 여부
  boolean existsByStartDateBetween(LocalDateTime weekStart, LocalDateTime weekEnd);

  // 지난주 생성된 투표 조회
  Optional<Vote> findByStartDateBetween(LocalDateTime startDate, LocalDateTime endDate);

  List<Vote> findTop2ByOrderByStartDateDesc();
}
