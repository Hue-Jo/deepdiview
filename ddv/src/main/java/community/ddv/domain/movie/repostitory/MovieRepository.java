package community.ddv.domain.movie.repostitory;

import community.ddv.domain.movie.entity.Movie;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

  // 넷플릭스 내 인기도 상위 n개의 영화정보 조회
  @Query(
      value =
          "SELECT * FROM movie WHERE is_available = true AND" +
          "(title REGEXP '[ㄱ-ㅎㅏ-ㅣ가-힣]' OR title REGEXP '^[a-zA-Z0-9\\s.,!?~&=\\\"'':()\\-\\+\\#\\*\\%\\/]+')" +
          "ORDER BY popularity DESC " +
          "LIMIT :limit",
      nativeQuery = true)
  List<Movie> findAllByAvailableIsTrueOrderByPopularityDesc(@Param("limit") int limit);

  // 특정 단어가 포함된 영화 정보 리스트 조회(공백 무시)
  @Query("""
      SELECT m
      FROM Movie m
      WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%')
      OR REPLACE(m.originalTitle, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%')
      ORDER BY m.popularity DESC
      """)
  Page<Movie> findByTitleFlexible(@Param("title") String title, Pageable pageable);

  // 제목 자동완성 5개
  @Query("""
      SELECT m.title
      FROM Movie m
      WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:keyword, ' ', ''), '%')
      ORDER BY m.popularity DESC
""")
      List<String> find5AutocompleteTitles(@Param("keyword") String title, Pageable pageable);

  // TMDB Id로 특정영화 조회
  Optional<Movie> findByTmdbId(Long tmdbId);

  @Query("""
      SELECT m 
      FROM Movie m 
      WHERE m.isAvailable = true 
        AND m.tmdbId NOT IN :excludedTmdbIds 
      ORDER BY m.popularity DESC
      """)
  List<Movie> findTop6RankExcludedTmdbIds(Set<Long> excludedTmdbIds, Pageable pageable);

}
