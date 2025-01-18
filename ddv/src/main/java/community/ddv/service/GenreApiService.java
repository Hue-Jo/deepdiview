package community.ddv.service;

import community.ddv.dto.GenreDTO;
import community.ddv.entity.Genre;
import community.ddv.repository.GenreRepository;
import community.ddv.response.GenreResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenreApiService {

  @Value("${tmdb.key}")
  private String tmdbKey;
  private final String TMDB_GENRE_API_URL = "https://api.themoviedb.org/3/genre/movie/list?language=ko-KR&api_key=";

  private final GenreRepository genreRepository;
  private final RestTemplate restTemplate;

  public void fetchAndSaveGenres() {

    try {
      // API 호출
      log.info("API 호출 시도");
      ResponseEntity<GenreResponse> response = restTemplate.getForEntity(TMDB_GENRE_API_URL + tmdbKey, GenreResponse.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        log.info("API로부터 성공적으로 응답 받았습니다.");

        // DTO에서 Entity로 변환 후 저장
        List<Genre> genres = response.getBody().getGenres()
            .stream()
            .map(GenreDTO::toEntity)
            .collect(Collectors.toList());

        // DB에 저장
        genreRepository.saveAll(genres);
        log.info("장르 정보가 DB에 성공적으로 저장되었습니다.");
      } else {
        log.warn("API로부터 응답을 받아오지 못 했습니다.");
      }
    } catch (RestClientException e) {
      log.error("API 호출 실패", e);
    } catch (RuntimeException e) {
      log.error("예상치 못한 예외 발생");
    }
  }

}
