package community.ddv.repository;

import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserAndMovie(User user, Movie movie);
  int countByUser_Id(Long userId);
  List<Review> findByMovie(Movie movie);
  //List<Review> findTop5ByMovieOrderByCreatedAtDesc(Movie movie);
}
