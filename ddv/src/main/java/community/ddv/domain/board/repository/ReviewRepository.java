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
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

  boolean existsByUserAndMovie(User user, Movie movie);

  int countByUser_Id(Long userId);

  Page<Review> findByMovie(Movie movie, Pageable pageable);

  List<Review> findByMovie(Movie movie);

  Page<Review> findByUser_Id(Long userId, Pageable pageable);

  Page<Review> findByUser_IdAndCertifiedTrue(Long userId, Pageable pageable);

  Page<Review> findByMovieAndCertifiedTrue(Movie movie, Pageable pageable);

  List<Review> findAllByUser_Id(Long userId);

  // 최신 리뷰 조회
  @Query(
      value = """
            select r from Review r
            join fetch r.user u
            join fetch r.movie m
          """,
      countQuery = "select count(r) from Review r"
  )
  Page<Review> findLatestReviews(Pageable pageable);

  Optional<Review> findByUserAndMovie(User user, Movie movie);

  @Query("select avg(r.rating) from Review r where r.movie = :movie")
  Double findAverageRatingByMovie(@Param("movie") Movie movie);

  // 특정 리뷰 조회 (댓글도 함께 조회)
  @Query("""
          select distinct r from Review r
          join fetch r.user
          join fetch r.movie
          left join fetch r.comments c
          left join fetch c.user
          where r.id = :reviewId
      """)
  Optional<Review> findWithCommentsById(@Param("reviewId") Long reviewId);
}
