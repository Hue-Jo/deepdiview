package community.ddv.domain.movie.repostitory;

import community.ddv.domain.movie.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

}
