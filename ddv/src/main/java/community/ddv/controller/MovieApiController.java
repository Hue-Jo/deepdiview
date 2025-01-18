package community.ddv.controller;

import community.ddv.service.MovieApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MovieApiController {

  private final MovieApiService movieApiService;

  // 장르 정보 저장
  @GetMapping("api/fetch-movies")
  public String fetchAndSaveMovies() {

    movieApiService.fetchAndSaveMovies();
    return "API로부터 영화세부 정보 받아오기 성공";
  }

}
