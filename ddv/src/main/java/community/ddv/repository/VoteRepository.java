package community.ddv.repository;

import community.ddv.entity.Vote;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

  // 투표가 진행중인지 확인 (시작일이 현재보다 먼저여야 하고, 마감일이 현재 이후여야 함
  Optional<Vote> findByStartDateBeforeAndEndDateAfter(LocalDateTime start, LocalDateTime end);
}
