package community.ddv.repository;

import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserAndMovie(User user, Movie movie);

}
