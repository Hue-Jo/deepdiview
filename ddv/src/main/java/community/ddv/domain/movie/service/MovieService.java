package community.ddv.domain.movie.service;

import community.ddv.domain.board.dto.ReviewRatingDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.board.service.ReviewService;
import community.ddv.domain.movie.dto.MovieDTO;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

  private final MovieRepository movieRepository;
  private final ReviewService reviewService;
  private final UserService userService;
  private final ReviewRepository reviewRepository;

  /**
   * 넷플릭스 내 인기도 탑 n 영화 세부정보 조회
   * @param size
   */
  public List<MovieDTO> getTopMovies(int size) {
    List<Movie> topMovies = movieRepository.findAllByAvailableIsTrueOrderByPopularityDesc(size);
    log.info("인기도 탑{} 영화 조회 성공", size);
    return topMovies.stream()
        .map(movie -> convertToDto(movie, null, reviewService.getRatingsByMovie(movie)))
        .collect(Collectors.toList());
  }

  /**
   * 넷플릭스 내 인기도 탑 20 영화 세부정보 조회
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "top20Movies", key = "'top20Movies'")
  public List<MovieDTO> getTop20Movies() {
    return getTopMovies(20);
  }

//  /**
//   * 넷플릭스 내 인기도 탑 6 영화 세부정보 조회
//   */
//  @Transactional(readOnly = true)
//  public List<MovieDTO> getTop6Movies() {
//    return getTopMovies(6);
//  }

  /**
   * 키워드로 영화 검색 _ 공백 무시 가능 & 특정 글자가 포함되는 조회됨 & 넷플 인기도 순 정렬
   * @param title
   */
  @Transactional(readOnly = true)
  public Page<MovieDTO> searchMoviesByTitle(String title, Pageable page) {

    Page<Movie> movies = movieRepository.findByTitleFlexible(title, page);
    if (movies.isEmpty()) {
      log.warn("키워드 '{}'를 포함하는 영화가 존재하지 않습니다.", title);
      return Page.empty(page);
    }

    log.info("영화 제목 '{}'으로 영화의 세부정보 조회 성공", title);
    return movies
        .map(movie -> {
          ReviewRatingDTO ratingStats = reviewService.getRatingsByMovie(movie);
          return convertToDto(movie, null, ratingStats);
        });
  }


  /**
   * 특정 영화 id로 해당 영화의 세부정보 조회
   * @param tmdbId
   */
  @Transactional(readOnly = true)
  public MovieDTO getMovieDetailsById(Long tmdbId, Boolean certifiedFilter) {

    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> {
          log.warn("영화 Id '{}'에 해당하는 영화가 없습니다.", tmdbId);
          return new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
        });

    ReviewResponseDTO myReview = null;
    User loginUser = userService.getLoginOrNull();
    if (loginUser != null) {
      Optional<Review> optionalReview = reviewRepository.findByUserAndMovie(loginUser, movie);
      myReview = optionalReview.map(reviewService::convertToReviewResponseDto).orElse(null);
    }

    return convertToDto(movie, myReview, reviewService.getRatingsByMovie(movie));
  }


  /**
   * 자동완성 5개 반환
   */
  @Transactional(readOnly = true)
  public List<String> autoCompleteTitles(String keyword) {
    Pageable page = PageRequest.of(0, 5);
    return movieRepository.find5AutocompleteTitles(keyword, page);
  }


  public MovieDTO convertToDto(
      Movie movie,
      ReviewResponseDTO myReview,
      ReviewRatingDTO ratingStats) {

    return MovieDTO.builder()
        .id(movie.getTmdbId())
        .title(movie.getTitle())
        .original_title(movie.getOriginalTitle())
        .overview(movie.getOverview())
        .release_date(movie.getReleaseDate())
        .runtime(movie.getRuntime())
        .popularity(movie.getPopularity())
        .poster_path(movie.getPosterPath())
        .backdrop_path(movie.getBackdropPath())
        .genre_ids(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getId())
            .collect(Collectors.toList()))
        .genre_names(movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getName())
            .collect(Collectors.toList()))
        .myReview(myReview)
        .ratingStats(ratingStats)
        .isAvailable(movie.isAvailable())
        .build();
  }

}
