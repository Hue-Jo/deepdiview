package community.ddv.response;

import community.ddv.dto.GenreDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class GenreResponse {

  private List<GenreDTO> genres;

}
