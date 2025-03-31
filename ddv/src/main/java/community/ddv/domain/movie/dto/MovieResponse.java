package community.ddv.domain.movie.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MovieResponse {

  private int total_pages;
  private List<MovieDTO> results;
}
