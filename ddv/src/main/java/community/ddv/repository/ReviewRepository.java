package community.ddv.repository;

import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserAndMovie(User user, Movie movie);
  int countByUser_Id(Long userId);
  Page<Review> findByMovie(Movie movie, Pageable pageable);
  Page<Review> findByUser_Id(Long userId, Pageable pageable);
  List<Review> findAllByUser_Id(Long userId);
  List<Review> findTop3ByOrderByCreatedAtDesc();
  Optional<Review> findByUserAndMovie(User user, Movie movie);
}
