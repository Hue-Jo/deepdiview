package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.ReviewRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
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
    Pageable pageable = PageRequest.of(0, size);
    Page<Movie> topMovies = movieRepository.findAllByOrderByPopularityDesc(pageable);
    log.info("인기도 탑{} 영화 조회 성공", size);
    return topMovies.stream()
        .map(this::convertToDtoWithoutReviews)
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

  /**
   * 넷플릭스 내 인기도 탑 5 영화 세부정보 조회
   */
  @Transactional(readOnly = true)
  public List<MovieDTO> getTop5Movies() {
    return getTopMovies(5);
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
          return convertToDtoWithReviews(movie, reviews.getContent());
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

    ReviewResponseDTO myReview = null;
    User loginUser = userService.getLoginOrNull();
    if (loginUser != null) {
      Optional<Review> optionalReview = reviewRepository.findByUserAndMovie(loginUser, movie);
      if (optionalReview.isPresent()) {
        myReview = reviewService.convertToReviewResponseDto(optionalReview.get());
      }
    }

    log.info("영화 tmdbId = '{}'로 영화의 세부정보 조회 성공", tmdbId);
    return convertToDtoWithReviewsAndMyReview(movie, reviews.getContent(), myReview);
  }


  // 리뷰 포함
  public MovieDTO convertToDtoWithReviews(Movie movie, List<ReviewResponseDTO> reviews) {
    return convertToDto(movie, reviews, null);
  }

  // 리뷰 포함 X
  public MovieDTO convertToDtoWithoutReviews(Movie movie) {
    return convertToDto(movie, Collections.emptyList(), null);
  }

  // 리뷰 & 내 리뷰 포함
  public MovieDTO convertToDtoWithReviewsAndMyReview(Movie movie, List<ReviewResponseDTO> reviews, ReviewResponseDTO myReview) {
    return convertToDto(movie, reviews, myReview);
  }


  public MovieDTO convertToDto(Movie movie, List<ReviewResponseDTO> reviews, ReviewResponseDTO myReview) {
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
        .myReview(myReview)
        .build();
  }
}
