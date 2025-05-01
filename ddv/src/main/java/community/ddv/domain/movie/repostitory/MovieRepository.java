package community.ddv.domain.movie.repostitory;

import community.ddv.domain.movie.entity.Movie;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

  // 넷플릭스 내 인기도 상위 n개의 영화정보 조회
  //Page<Movie> findAllByOrderByPopularityDesc(Pageable pageable);
  Page<Movie> findAllByAvailableIsTrueOrderByPopularityDesc(Pageable pageable);

  // 특정 단어가 포함된 영화 정보 리스트 조회(공백 무시)
  //@Query("SELECT m FROM Movie m WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%') AND m.isAvailable = true ORDER BY m.popularity DESC")
  @Query("SELECT m FROM Movie m WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%') ORDER BY m.popularity DESC")
  Page<Movie> findByTitleFlexible(@Param("title") String title, Pageable pageable);

  // TMDB Id로 특정영화 조회
  Optional<Movie> findByTmdbId(Long tmdbId);

}
