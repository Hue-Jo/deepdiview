package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.entity.Movie;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

  private final MovieRepository movieRepository;
  private final ReviewService reviewService;

  /**
   * 넷플릭스 내 인기도 탑 20 영화 세부정보 조회
   */
  public List<MovieDTO> getTop20Movies() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Movie> top20Movies = movieRepository.findTop20ByOrderByPopularityDesc(pageable);
    log.info("인기도 탑20 영화 조회 성공");
    return top20Movies.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }

  /**
   * 넷플릭스 내 인기도 탑 5 영화 세부정보 조회
   */
  public List<MovieDTO> getTop5Movies() {
    Pageable pageable = PageRequest.of(0, 5);
    Page<Movie> top5Movies = movieRepository.findTop5ByOrderByPopularityDesc(pageable);
    log.info("인기도 탑5 영화 조회 성공");
    return top5Movies.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }


  /**
   * 영화 제목으로 해당 영화의 세부정보 조회 _ 공백 무시 가능 & 특정 글자가 포함되는 조회됨 & 넷플 인기도 순 정렬
   * @param title
   */
  public List<MovieDTO> searchMoviesByTitle(String title, Boolean certifiedFilter) {

    List<Movie> movies = movieRepository.findByTitleFlexible(title);

    if (movies.isEmpty()) {
      log.warn("키워드 '{}'를 포함하는 영화가 존재하지 않습니다.", title);
      throw new DeepdiviewException(ErrorCode.KEYWORD_NOT_FOUND);
    }

    log.info("영화 제목 '{}'으로 영화의 세부정보 조회 성공", title);
    return movies.stream()
        .map(movie -> {
          Pageable pageable = PageRequest.of(0, 5, Sort.by(Direction.DESC, "createdAt"));
          Page<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(movie.getTmdbId(), pageable, certifiedFilter);
          return convertToDTOwithReviews(movie, reviews.getContent());
        })
        .collect(Collectors.toList());
  }

  /**
   * 특정 영화 id로 해당 영화의 세부정보 조회
   * @param tmdbId
   */
  public MovieDTO getMovieDetailsById(Long tmdbId, Boolean certifiedFilter) {
    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> {
          log.warn("영화 Id {}에 해당하는 영화가 없습니다.", tmdbId);
          return new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
        });
    Pageable pageable = PageRequest.of(0, 5, Sort.by(Direction.DESC, "createdAt"));
    Page<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(tmdbId, pageable, certifiedFilter);
    log.info("영화 tmdbId = '{}'로 영화의 세부정보 조회 성공", tmdbId);
    return convertToDTOwithReviews(movie, reviews.getContent());
  }


  public MovieDTO convertToDTOwithReviews(Movie movie, List<ReviewResponseDTO> reviews) {

    return MovieDTO.builder()
        .id(movie.getTmdbId())
        .title(movie.getTitle())
        .original_title(movie.getOriginalTitle())
        .overview(movie.getOverview())
        .release_date(movie.getReleaseDate())
        .popularity(movie.getPopularity())
        .poster_path(movie.getPosterPath())
        .backdrop_path(movie.getBackdropPath())
        .genre_ids(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getId())
            .collect(Collectors.toList()))
        .genre_names(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getName())
            .collect(Collectors.toList()))
        .reviews(reviews)
        .build();
  }

  public MovieDTO convertToDTO(Movie movie) {

    return MovieDTO.builder()
        .id(movie.getTmdbId())
        .title(movie.getTitle())
        .original_title(movie.getOriginalTitle())
        .overview(movie.getOverview())
        .release_date(movie.getReleaseDate())
        .popularity(movie.getPopularity())
        .poster_path(movie.getPosterPath())
        .backdrop_path(movie.getBackdropPath())
        .genre_ids(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getId())
            .collect(Collectors.toList()))
        .genre_names(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getName())
            .collect(Collectors.toList()))
        .build();
  }
}
