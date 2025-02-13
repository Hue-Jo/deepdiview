package community.ddv.repository;

import community.ddv.entity.Like;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
  Optional<Like> findByUserAndReview(User user, Review review);

}
