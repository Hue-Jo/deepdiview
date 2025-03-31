package community.ddv.domain.vote.repository;

import community.ddv.domain.user.entity.User;
import community.ddv.domain.vote.entity.Vote;
import community.ddv.domain.vote.entity.VoteParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {

  boolean existsByUserAndVote(User user, Vote vote);

}
