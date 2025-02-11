package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.entity.Movie;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    return top5Movies.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }


  /**
   * 영화 제목으로 해당 영화의 세부정보 조회 _ 공백 무시 가능 & 특정 글자가 포함되는 조회됨 & 넷플 인기도 순 정렬
   * @param title
   */
  public List<MovieDTO> searchMoviesByTitle(String title) {
    try {
      List<Movie> movies = movieRepository.findByTitleFlexible(title);
      if (movies.isEmpty()) {
        throw new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
      }
      return movies.stream()
          .map(movie -> {
            List<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(movie.getTmdbId());
            // 최신순 5개만 가져오도록 제한
            List<ReviewResponseDTO> latestReviews = reviews.stream()
                .sorted(Comparator.comparing(ReviewResponseDTO::getCreatedAt).reversed())
                .limit(5)
                .collect(Collectors.toList());
            return convertToDTOwithReviews(movie, latestReviews);
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("영화 정보 조회 중 오류 발생");
    }
  }

  /**
   * 특정 영화 id로 해당 영화의 세부정보 조회
   * @param tmdbId
   */
  public MovieDTO getMovieDetailsById(Long tmdbId) {
    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));
    List<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(tmdbId);
    List<ReviewResponseDTO> latestReviews = reviews.stream()
        .sorted(Comparator.comparing(ReviewResponseDTO::getCreatedAt).reversed())
        .limit(5)
        .collect(Collectors.toList());
    return convertToDTOwithReviews(movie, latestReviews);
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
