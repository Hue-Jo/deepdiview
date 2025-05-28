package community.ddv.domain.movie.service;

import community.ddv.domain.movie.dto.MovieDTO;
import community.ddv.domain.movie.dto.MovieResponse;
import community.ddv.domain.movie.entity.Genre;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.movie.repostitory.GenreRepository;
import community.ddv.domain.movie.repostitory.MovieRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieApiService {

  @Value("${tmdb.key}")
  private String tmdbKey;
  private final String TMDB_MOVIE_API_URL = "https://api.themoviedb.org/3/discover/movie?include_adult=true&include_video=false&language=ko&sort_by=primary_release_date.desc&watch_region=KR&with_watch_providers=8&api_key=";
  private final String TMDB_MOVIE_RUNTIME_API_URL = "https://api.themoviedb.org/3/movie/";

  private final MovieRepository movieRepository;
  private final GenreRepository genreRepository;
  private final RestTemplate restTemplate;

  public void fetchAndSaveMovies() {

    int currentPage = 1;
    int totalPages = 1;
    Set<Long> fetchedTmdbIds = new HashSet<>();

    // 기존에 존재하던 영화들은 Map에 넣어둠 (반복문에서 사용되지 않도록 밖으로 빼냄)
    Map<Long, Movie> existingMovies = movieRepository.findAll().stream()
        .collect(Collectors.toMap(Movie::getTmdbId, Function.identity()));

    Map<Long, Genre> genreMap = genreRepository.findAll().stream()
        .collect(Collectors.toMap(Genre::getId, Function.identity()));

    boolean hasNext = true;
    while (hasNext) {
      try {
        // API 호출
        String url = TMDB_MOVIE_API_URL + tmdbKey + "&page=" + currentPage;
        ResponseEntity<MovieResponse> response = restTemplate.getForEntity(url, MovieResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() && response.getBody() == null) {
          log.error("API로부터 응답을 받아오지 못했습니다.");
          break;
        }

        MovieResponse movieResponse = response.getBody();
        totalPages = movieResponse.getTotal_pages();

        // DTO에서 Entity로 변환 후 저장
        List<Movie> movies = response.getBody().getResults().stream()
            .map(movieDto -> toMovieEntity(movieDto, genreMap))
            .toList();

        for (Movie movie : movies) {
          fetchedTmdbIds.add(movie.getTmdbId());

          // 이미 DB에 존재하는 TMDB ID인지 확인
          Movie existingMovie = existingMovies.get(movie.getTmdbId());
          // 이미 존재한다면, 인기도와 현재 제공중인지 여부만 업데이트 후 저장
          if (existingMovie != null) {
            existingMovie.setPopularity(movie.getPopularity());
            existingMovie.setAvailable(true);
            movieRepository.save(existingMovie);
          } else {
            // 존재하지 않는다면 새로 저장
            movieRepository.save(movie);
          }
        }
        log.info("영화 정보가 DB에 성공적으로 저장되었습니다. 페이지 " + currentPage + "/" + totalPages);

        currentPage++;
        hasNext = currentPage <= totalPages;

      } catch (RestClientException e) {
        log.error("API 호출 실패", e);
        break;
      } catch (RuntimeException e) {
        log.error("예상치 못한 예외 발생", e);
        break;
      }
    }

    // DB에는 있지만 API에서 사라진 영화는 isAvailable = false로 변경
    for (Movie movie : existingMovies.values()) {
      if (!fetchedTmdbIds.contains(movie.getTmdbId())) {
        movie.changeAsUnavailable();
        movieRepository.save(movie);
      }
    }
    log.info("모든 영화 데이터 저장/업데이트 완료 ");
  }

  public void fetchMovieRunTime() {
    log.info("영화 런타임 업데이트 시작");
    List<Movie> movies = movieRepository.findAll();

    for (Movie movie : movies) {
      // 이미 런타임 정보가 있는 경우, continue
      if (movie.getRuntime() != null) {
        continue;
      }

      try {
        String runtimeUrl = TMDB_MOVIE_RUNTIME_API_URL + movie.getTmdbId() + "?api_key=" + tmdbKey;
        ResponseEntity<MovieDTO> response = restTemplate.getForEntity(runtimeUrl, MovieDTO.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
          Integer runtime = response.getBody().getRuntime();
          if (runtime == null) {
            movie.updateRuntime(null);
          } else {
            movie.updateRuntime(runtime);
          }
          movieRepository.save(movie);
        } else {
          log.warn("런타임 응답 실패 - " + movie.getTmdbId());
        }

      } catch (HttpClientErrorException e) {
        log.warn("런타임 정보를 찾을 수 없음: TMDB ID {}", movie.getTmdbId());
      } catch (RestClientException e) {
        log.error("런타임 정보 요청 실패: TMDB ID {}", movie.getTmdbId(), e);
      } catch (Exception e) {
        log.error("예기치 못한 예외 발생: TMDB ID {}", movie.getTmdbId(), e);
      }
    }
    log.info("런타임 정보 업데이트 완료");
  }


  private Movie toMovieEntity(MovieDTO movieDTO, Map<Long, Genre> genreMap) {
    Movie movie = Movie.builder()
        .tmdbId(movieDTO.getId())
        .title(movieDTO.getTitle())
        .originalTitle(movieDTO.getOriginal_title())
        .overview(movieDTO.getOverview())
        .releaseDate(movieDTO.getRelease_date())
        .runtime(movieDTO.getRuntime())
        .popularity(movieDTO.getPopularity())
        .posterPath(movieDTO.getPoster_path())
        .backdropPath(movieDTO.getBackdrop_path())
        .build();

    for (Long genreId : movieDTO.getGenre_ids()) {
      Genre genre = genreMap.get(genreId);
      if (genre == null) {
        throw new RuntimeException("장르를 찾을 수 없습니다. ID: " + genreId);
      }
      movie.addGenre(genre);
    }
    return movie;
  }

}
