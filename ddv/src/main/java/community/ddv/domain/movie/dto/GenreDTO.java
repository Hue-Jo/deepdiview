package community.ddv.domain.movie.dto;

import community.ddv.domain.movie.entity.Genre;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GenreDTO {

  private Long id;
  private String name;

  public Genre toEntity() {
    return new Genre(id, name);
  }

}
