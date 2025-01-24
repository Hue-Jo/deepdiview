package community.ddv.repository;

import community.ddv.entity.Movie;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

  // 넷플 내 인기도 상위 20개의 영화 정보 조회 (페이징처리)
  Page<Movie> findTop20ByOrderByPopularityDesc(Pageable pageable);

  // 특정 단어가 포함된 영화 정보 리스트 조회(공백 무시)
  @Query("SELECT m FROM Movie m WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%') ORDER BY m.popularity DESC")
  List<Movie> findByTitleFlexible(@Param("title") String title);

  // 특정 영화 세부정보 조회 (공백 무시)
  @Query("SELECT m FROM Movie m WHERE REPLACE(m.title, ' ', '') LIKE CONCAT('%', REPLACE(:title, ' ', ''), '%') ORDER BY m.popularity DESC")
  Optional<Movie> findByTitle(@Param("title") String title);

}
