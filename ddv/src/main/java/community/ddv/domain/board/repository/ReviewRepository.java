package community.ddv.domain.board.repository;

import community.ddv.domain.board.entity.Review;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserAndMovie(User user, Movie movie);
  int countByUser_Id(Long userId);
  Page<Review> findByMovie(Movie movie, Pageable pageable);
  List<Review> findByMovie(Movie movie);
  Page<Review> findByUser_Id(Long userId, Pageable pageable);
  Page<Review> findByUser_IdAndCertifiedTrue(Long userId, Pageable pageable);
  Page<Review> findByMovieAndCertifiedTrue(Movie movie, Pageable pageable);
  List<Review> findAllByUser_Id(Long userId);
  //Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
  @Query(
      value = """
    select distinct r from Review r
    join fetch r.user u
    join fetch r.movie m
  """,
      countQuery = "select count(r) from Review r"
  )
  Page<Review> findLatestReviews(Pageable pageable);
  Optional<Review> findByUserAndMovie(User user, Movie movie);
}
