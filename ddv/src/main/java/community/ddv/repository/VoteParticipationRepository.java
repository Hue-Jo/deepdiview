package community.ddv.repository;

import community.ddv.entity.User;
import community.ddv.entity.Vote;
import community.ddv.entity.VoteParticipation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {

  boolean existsByUserAndVote(User user, Vote vote);

  List<VoteParticipation> findByVoteOrderByVotedAtDesc(Vote vote);
}
