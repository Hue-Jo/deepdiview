package community.ddv.domain.vote.repository;

import community.ddv.domain.vote.entity.VoteMovie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteMovieRepository extends JpaRepository<VoteMovie, Long> {

}
