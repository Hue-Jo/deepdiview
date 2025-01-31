package community.ddv.repository;

import community.ddv.entity.User;
import community.ddv.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

  // 투표 참여 여부 확인
  boolean existsByUser(User user);
}
