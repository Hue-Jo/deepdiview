package community.ddv.controller;

import community.ddv.service.GenreApiService;
import community.ddv.service.MovieApiService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fetch")
@Hidden
public class ApiController {

  private final GenreApiService genreApiService;
  private final MovieApiService movieApiService;

  // 장르 정보 저장
  @Operation(summary = "DB 초기화했을 때 1순위 저장", description = "평소에는 사용하지 않으셔도 됩니다.")
  @GetMapping("/genres")
  public String fetchAndSaveGenres() {

    genreApiService.fetchAndSaveGenres();
    return "API로부터 장르 정보 받아오기 성공";
  }

  // 장르 정보 저장
  @Operation(summary = "DB 초기화했을 때 후순위 저장", description = "평소에는 사용하지 않으셔도 됩니다.")
  @GetMapping("/movies")
  public String fetchAndSaveMovies() {

    movieApiService.fetchAndSaveMovies();
    return "API로부터 영화세부 정보 받아오기 성공";
  }

}
