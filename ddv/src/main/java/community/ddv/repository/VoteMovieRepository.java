package community.ddv.repository;

import community.ddv.entity.VoteMovie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteMovieRepository extends JpaRepository<VoteMovie, Long> {

}
