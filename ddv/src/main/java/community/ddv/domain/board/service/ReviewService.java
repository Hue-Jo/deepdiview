package community.ddv.domain.board.service;

import community.ddv.domain.board.dto.ReviewDTO;
import community.ddv.domain.board.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.domain.board.dto.ReviewIdResponseDto;
import community.ddv.domain.board.dto.ReviewRatingDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.CommentRepository;
import community.ddv.domain.board.repository.LikeRepository;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import community.ddv.global.response.PageResponse;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final MovieRepository movieRepository;
  private final UserService userService;
  private final forbiddenWordsFilter forbiddenWordsFilter;
  private final CommentRepository commentRepository;
  private final LikeRepository likeRepository;

  /**
   * 영화 리뷰 작성 _ 유저는 특정 영화에 대해 한 번만 리뷰 작성 가능
   * @param reviewDTO
   */
  @Transactional
  public ReviewIdResponseDto createReview(ReviewDTO reviewDTO) {

    User user = userService.getLoginUser();

    Movie movie = movieRepository.findByTmdbId(reviewDTO.getTmdbId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    if (reviewRepository.existsByUserAndMovie(user, movie)) {
      log.warn("리뷰 작성 실패 (이미 리뷰를 작성함) - User Id : {}, TMDB Id: {} ", user.getId(), reviewDTO.getTmdbId());
      throw new DeepdiviewException(ErrorCode.ALREADY_COMMITTED_REVIEW);
    }

    String filteredTitle = forbiddenWordsFilter.filterForbiddenWords(reviewDTO.getTitle());
    String filteredContent = forbiddenWordsFilter.filterForbiddenWords(reviewDTO.getContent());

    Review review = Review.builder()
        .user(user)
        .movie(movie)
        .tmdbId(movie.getTmdbId())
        .title(filteredTitle)
        .content(filteredContent)
        .rating(reviewDTO.getRating())
        .likeCount(0)
        .certified(reviewDTO.isCertified())
        .build();

    reviewRepository.save(review);
    log.info("리뷰 작성 성공 - 사용자 ID: {}, 리뷰 ID: {}", user.getId(), review.getId());
    return new ReviewIdResponseDto(review.getId());
  }

  /**
   * 리뷰 삭제 _ 작성자만 가능
   * @param reviewId
   */
  @Transactional
  public void deleteReview(Long reviewId) {

    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().equals(user)) {
      log.warn("리뷰 삭제 권한 없음 - 리뷰 ID: {}, 삭제 요청 유저 ID: {}, 리뷰 작성자 ID: {}", reviewId, user.getId(),
          review.getUser().getId());
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }
    reviewRepository.delete(review);
  }

  /**
   * 리뷰 수정 _ 작성자만 수정 가능, 제목 내용 별점 각각 수정 가능, 별점은 변경하지 않을 시 null로 들어가야 함
   * @param reviewId
   * @param reviewUpdateDTO
   * @return
   */
  @Transactional
  public ReviewIdResponseDto updateReview(Long reviewId, ReviewUpdateDTO reviewUpdateDTO) {

    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().equals(user)) {
      log.warn("리뷰 수정 권한 없음 - 유저 ID: {}, 리뷰 ID: {}, ", user.getId(), reviewId);
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    String filteredTitle = forbiddenWordsFilter.filterForbiddenWords(reviewUpdateDTO.getTitle());
    String filteredContent = forbiddenWordsFilter.filterForbiddenWords(reviewUpdateDTO.getContent());

    review.updateReview(
        filteredTitle,
        filteredContent,
        reviewUpdateDTO.getRating()
    );
    log.info("리뷰 수정 성공 - 리뷰 ID: {}", reviewId);
    return new ReviewIdResponseDto(review.getId());

  }

  /**
   * 특정 영화의 리뷰 조회
   * @param tmdbId
   */
  @Transactional(readOnly = true)
  public Page<ReviewResponseDTO> getReviewByMovieId(Long tmdbId, Pageable pageable, Boolean certifiedFilter) {

    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> {
          log.warn("영화 조회 실패 - TMDB ID : {}", tmdbId);
          return new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
        });

    Page<Review> reviews;

    if (certifiedFilter) {
      log.info("인증된 리뷰만 조회");
      reviews = reviewRepository.findByMovieAndCertifiedTrue(movie, pageable);
    } else {
      reviews = reviewRepository.findByMovie(movie, pageable);
    }

    return reviews.map(this::convertToReviewResponseDto);
  }

  /**
   * 특정 리뷰 조회
   * @param reviewId
   */
  @Transactional(readOnly = true)
  public ReviewResponseDTO getReviewById(Long reviewId) {
    //Review review = reviewRepository.findById(reviewId)
    Review review = reviewRepository.findWithCommentsById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));
    return convertToReviewResponseDto(review);
  }

  /**
   * 특정 사용자가 작성한 리뷰 조회
   *
   * @param userId
   * @return
   */
  @Transactional(readOnly = true)
  public Page<ReviewResponseDTO> getReviewsByUserId(Long userId, Pageable pageable, Boolean certifiedFilter) {

    Page<Review> reviews;

    if (certifiedFilter) {
      log.info("특정 사용자의 인증된 리뷰만 조회");
      reviews = reviewRepository.findByUser_IdAndCertifiedTrue(userId, pageable);
    } else {
      log.info("특정 사용자의 모든 리뷰 조회");
      reviews = reviewRepository.findByUser_Id(userId, pageable);
    }

    return reviews.map(this::convertToReviewResponseDto);

  }


  /**
   * 최신순 리뷰 n개 조회
   */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponseDTO> getLatestReviews(Pageable pageable) {
    Page<Review> reviews = reviewRepository.findLatestReviews(pageable);
    Page<ReviewResponseDTO> reviewResponseDTOS = reviews.map(this::convertToReviewResponseDto);
    return new PageResponse<>(reviewResponseDTOS);
  }


  /**
   * 특정 영화의 평균별점, 별점 분포 조회 메서드
   */
  public ReviewRatingDTO getRatingsByMovie(Movie movie) {

    // 평균 별점
    Double ratingAverage = reviewRepository.findAverageRatingByMovie(movie);
    double roundedRatingAverage = ratingAverage == null ? 0.0 : Math.round(ratingAverage * 10.0) / 10.0;

    // 별점 분포
    List<Review> reviews = reviewRepository.findByMovie(movie);
    Map<Double, Integer> ratingDistribution = initializeRatingDistribution(reviews);

    return new ReviewRatingDTO(roundedRatingAverage, ratingDistribution);
  }

  // 별점 분포 초기화 메서드
  private Map<Double, Integer> initializeRatingDistribution(List<Review> reviews) {

    // 0.5 - 5.0 순서대로 출력하기 위해 LinkedHashMap 사용 ("0.5": 0 이렇게 매핑)
    Map<Double, Integer> ratingDistribution = new LinkedHashMap<>();
    for (double i = 0.5; i <= 5.0; i += 0.5) {
      ratingDistribution.put(i, 0);
    }

    reviews.stream()
        .map(Review::getRating)
        .filter(Objects::nonNull)
        .forEach(rating -> ratingDistribution.computeIfPresent(rating, (key, value) -> value + 1));

    return ratingDistribution;
  }


  public ReviewResponseDTO convertToReviewResponseDto(Review review) {

    User loginUser = userService.getLoginOrNull();
    Boolean likedByUser = (loginUser != null)
        ? likeRepository.existsByReviewAndUser(review, loginUser)
        : null;

    // 댓글 개수
    int commentCount = commentRepository.countByReview(review);

    return ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .nickname(review.getUser().getNickname())
        .profileImageUrl(review.getUser().getProfileImageUrl())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt().atOffset(ZoneOffset.of("+09:00")))
        .updatedAt(review.getUpdatedAt().atOffset(ZoneOffset.of("+09:00")))
        .commentCount(commentCount)
        .likeCount(review.getLikeCount())
        .likedByUser(likedByUser)
        .tmdbId(review.getMovie().getTmdbId())
        .movieTitle(review.getMovie().getTitle())
        .posterPath(review.getMovie().getPosterPath())
        .certified(review.isCertified())
        .build();
  }
}
