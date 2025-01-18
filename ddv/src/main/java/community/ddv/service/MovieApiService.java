package community.ddv.service;

import community.ddv.dto.MovieDTO;
import community.ddv.entity.Genre;
import community.ddv.entity.Movie;
import community.ddv.repository.GenreRepository;
import community.ddv.repository.MovieGenreRepository;
import community.ddv.repository.MovieRepository;
import community.ddv.response.MovieResponse;
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
public class MovieApiService {

  @Value("${tmdb.key}")
  private String tmdbKey;
  private final String TMDB_MOVIE_API_URL = "https://api.themoviedb.org/3/discover/movie?include_adult=true&include_video=false&language=ko-KR&region=KR&sort_by=popularity.desc&watch_region=KR&with_watch_providers=8&api_key=";

  private final MovieRepository movieRepository;
  private final GenreRepository genreRepository;
  private final RestTemplate restTemplate;

  public void fetchAndSaveMovies() {

    int currentPage = 1;
    int totalPages;

    do {
      try {
        // API 호출
        String url = TMDB_MOVIE_API_URL + tmdbKey + "&page=" + currentPage;
        ResponseEntity<MovieResponse> response = restTemplate.getForEntity(url,
            MovieResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

          MovieResponse movieResponse = response.getBody();
          totalPages = movieResponse.getTotal_pages();

          // DTO에서 Entity로 변환 후 저장
          List<Movie> movies = response.getBody().getResults()
              .stream()
              .map(this::toEntity)
              .collect(Collectors.toList());

          movieRepository.saveAll(movies);
          log.info("영화 정보가 DB에 성공적으로 저장되었습니다. 페이지 " + currentPage + "/" + totalPages);
          currentPage++;

        } else {
          log.error("API로부터 응답을 받아오지 못 했습니다.");
          break;
        }
      } catch (RestClientException e) {
        log.error("API 호출 실패", e);
        break;
      } catch (RuntimeException e) {
        log.error("예상치 못한 예외 발생", e);
        break;
      }
    } while (currentPage <= totalPages);
    log.info("모든 영화 데이터 저장 완료 ");

  }

  private Movie toEntity(MovieDTO movieDTO) {
    Movie movie = Movie.builder()
        .timdbId(movieDTO.getId())
        .title(movieDTO.getTitle())
        .originalTitle(movieDTO.getOriginal_title())
        .overview(movieDTO.getOverview())
        .releaseDate(movieDTO.getRelease_date())
        .popularity(movieDTO.getPopularity())
        .posterPath(movieDTO.getPoster_path())
        .backdropPath(movieDTO.getBackdrop_path())
        .build();

    for (Long genreId : movieDTO.getGenre_ids()) {
      Genre genre = genreRepository.findById(genreId).orElseThrow(() -> new RuntimeException("장르를 찾을 수 없습니다."));
      movie.addGenre(genre);
    }
    return movie;
  }

}
