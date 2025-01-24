package community.ddv.controller;

import community.ddv.dto.MovieDTO;
import community.ddv.service.MovieService;
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

  @GetMapping("/popularity/top20")
  public ResponseEntity<List<MovieDTO>> showTop20Movie() {
    List<MovieDTO> top20Movies = movieService.getTop20Movies();
    return ResponseEntity.ok(top20Movies);
  }

  @GetMapping("/search/list")
  public ResponseEntity<List<MovieDTO>> getMoviesByTitle(@RequestParam("title") String title) {
    List<MovieDTO> movies = movieService.searchMoviesByTitle(title);
    return ResponseEntity.ok(movies);
  }

  @GetMapping("/{movieId}")
  public ResponseEntity<MovieDTO> getMovieDetail(@PathVariable Long movieId) {
    MovieDTO movieDetails = movieService.getMovieDetailsById(movieId);
    return ResponseEntity.ok(movieDetails);
  }
}
