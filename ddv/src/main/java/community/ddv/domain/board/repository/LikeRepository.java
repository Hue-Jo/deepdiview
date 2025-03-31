package community.ddv.domain.board.repository;

import community.ddv.domain.board.entity.Like;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
  Optional<Like> findByUserAndReview(User user, Review review);

}
