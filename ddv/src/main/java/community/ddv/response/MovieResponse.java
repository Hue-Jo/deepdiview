package community.ddv.response;

import community.ddv.dto.MovieDTO;
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
