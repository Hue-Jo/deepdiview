package community.ddv.controller;

import community.ddv.dto.MovieDTO;
import community.ddv.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/movies")
public class MovieController {

  private final MovieService movieService;

  @Operation(summary = "넷플릭스 인기도 탑20의 영화 리스트 조회")
  @GetMapping("/popularity/top20")
  public ResponseEntity<List<MovieDTO>> showTop20Movie() {
    List<MovieDTO> top20Movies = movieService.getTop20Movies();
    return ResponseEntity.ok(top20Movies);
  }

  @Operation(summary = "영화 제목으로 상세정보 조회", description = "특정 단어가 포함되어 있는 영화들의 세부정보를 반환합니다. 띄어쓰기를 무시하고도 조회가 됩니다.")
  @GetMapping("/search/list")
  public ResponseEntity<List<MovieDTO>> getMoviesByTitle(
      @RequestParam("title") String title,
      @RequestParam(value = "certifiedFilter", required = false, defaultValue = "false") Boolean certifiedFilter
      ) {
    List<MovieDTO> movies = movieService.searchMoviesByTitle(title, certifiedFilter);
    return ResponseEntity.ok(movies);
  }

  @Operation(summary = "특정 영화 상세정보 조회", description = "tmdb에서 제공하는 id값(tmdbId)을 넣어야 조회됩니다.")
  @GetMapping("/{movieId}")
  public ResponseEntity<MovieDTO> getMovieDetail(
      @PathVariable Long movieId,
      @RequestParam(value = "certifiedFilter", required = false, defaultValue = "false") Boolean certifiedFilter
      ) {
    MovieDTO movieDetails = movieService.getMovieDetailsById(movieId, certifiedFilter);
    return ResponseEntity.ok(movieDetails);
  }
}
