package community.ddv.controller;

import community.ddv.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenreController {

  private final GenreService genreService;

  // 장르 정보 저장
  @GetMapping("api/fetch-genres")
  public String fetchGenres() {

    genreService.fetchAndSaveGenres();
    return "API로부터 장르 정보 받아오기 성공";
  }

}
