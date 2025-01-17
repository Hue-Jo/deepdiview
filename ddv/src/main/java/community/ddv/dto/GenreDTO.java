package community.ddv.dto;

import community.ddv.entity.Genre;
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
