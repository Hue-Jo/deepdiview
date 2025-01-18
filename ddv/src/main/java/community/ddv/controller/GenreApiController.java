package community.ddv.controller;

import community.ddv.service.GenreApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenreApiController {

  private final GenreApiService genreApiService;

  // 장르 정보 저장
  @GetMapping("api/fetch-genres")
  public String fetchAndSaveGenres() {

    genreApiService.fetchAndSaveGenres();
    return "API로부터 장르 정보 받아오기 성공";
  }

}
