package community.ddv.domain.board.service;

import community.ddv.domain.board.dto.CommentDTO.CommentResponseDto;
import community.ddv.domain.board.dto.ReviewDTO;
import community.ddv.domain.board.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.domain.board.dto.ReviewRatingDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.entity.Comment;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import community.ddv.global.response.PageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

  /**
   * 영화 리뷰 작성 _ 유저는 특정 영화에 대해 한 번만 리뷰 작성 가능
   * @param reviewDTO
   */
  @Transactional
  public ReviewResponseDTO createReview(ReviewDTO reviewDTO) {

    User user = userService.getLoginUser();

    Movie movie = movieRepository.findByTmdbId(reviewDTO.getTmdbId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    if (reviewRepository.existsByUserAndMovie(user, movie)) {
      log.warn("리뷰 작성 실패 (이미 리뷰를 작성함) - User Id : {}, TMDB Id: {} ", user.getId(), reviewDTO.getTmdbId());
      throw new DeepdiviewException(ErrorCode.ALREADY_COMMITTED_REVIEW);
    }

    Review review = Review.builder()
        .user(user)
        .movie(movie)
        .tmdbId(movie.getTmdbId())
        .title(reviewDTO.getTitle())
        .content(reviewDTO.getContent())
        .rating(reviewDTO.getRating())
        .likeCount(0)
        .certified(reviewDTO.isCertified())
        .build();

    reviewRepository.save(review);
    return convertToReviewResponseWithoutCommentsDto(review);
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
  public ReviewResponseDTO updateReview(Long reviewId, ReviewUpdateDTO reviewUpdateDTO) {

    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().equals(user)) {
      log.warn("리뷰 수정 권한 없음 - 유저 ID: {}, 리뷰 ID: {}, ", user.getId(), reviewId);
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    review.updateReview(
        reviewUpdateDTO.getTitle(),
        reviewUpdateDTO.getContent(),
        reviewUpdateDTO.getRating()
    );

    return convertToReviewResponseWithoutCommentsDto(review);

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

    return reviews.map(this::convertToReviewResponseWithoutCommentsDto);
  }

  /**
   * 특정 리뷰 조회
   * @param reviewId
   */
  @Transactional(readOnly = true)
  public ReviewResponseDTO getReviewById(Long reviewId) {

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> {
          log.warn("리뷰 조회 실패 - 리뷰 ID : {}", reviewId);
          return new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND);
        });

    return convertToReviewResponseWithCommentsDto(review);
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

    return reviews.map(this::convertToReviewResponseWithoutCommentsDto);

  }


  /**
   * 최신순 리뷰 n개 조회
   */
  @Transactional(readOnly = true)
  public PageResponse<ReviewResponseDTO> getLatestReviews(Pageable pageable) {
    Page<Review> reviews = reviewRepository.findLatestReviews(pageable);
    Page<ReviewResponseDTO> reviewResponseDTOS = reviews.map(this::convertToReviewResponseWithoutCommentsDto);
    return new PageResponse<>(reviewResponseDTOS);
  }


  // 특정 영화의 평균별점, 별점 분포 조회 메서드
  public ReviewRatingDTO getRatingsByMovie(Movie movie) {

//    if (reviews.isEmpty()) {
//      return new ReviewRatingDTO(0.0, initializeRatingDistribution());
//    }
//
//    double ratingAverage = reviews.stream()
//        .map(Review::getRating)
//        .filter(Objects::nonNull)
//        .mapToDouble(Double::doubleValue)
//        .average()
//        .orElse(0.0);

    // 평균 별점
    Double ratingAverage = reviewRepository.findAverageRatingByMovie(movie);
    double roundedRatingAverage = ratingAverage == null ? 0.0 : Math.round(ratingAverage * 100) / 100.0;

    List<Review> reviews = reviewRepository.findByMovie(movie);

    // 별점 분포
    Map<Double, Integer> ratingDistribution = initializeRatingDistribution(); // 별점 분포도
    reviews.stream()
        .map(Review::getRating)
        .filter(Objects::nonNull)
        .forEach(rating -> ratingDistribution.put(rating, ratingDistribution.get(rating) + 1));

    return new ReviewRatingDTO(roundedRatingAverage, ratingDistribution);
  }

  // 별점 분포 초기화 메서드
  private Map<Double, Integer> initializeRatingDistribution() {
    // 0.5 - 5.0 순서대로 출력하기 위해 LinkedHashMap 사용 ("0.5": 0 이렇게 매핑)
    Map<Double, Integer> ratingDistribution = new LinkedHashMap<>();
    for (double i = 0.5; i <= 5.0; i += 0.5) {
      ratingDistribution.put(i, 0);
    }
    return ratingDistribution;
  }


  // 댓글 포함  X
  public ReviewResponseDTO convertToReviewResponseWithoutCommentsDto(Review review) {
    return convertToReviewResponseDtoBase(review, false);
  }

  // 댓글 포함  O
  public ReviewResponseDTO convertToReviewResponseWithCommentsDto(Review review) {
    return convertToReviewResponseDtoBase(review, true);
  }

  private ReviewResponseDTO convertToReviewResponseDtoBase(Review review, boolean includeComments) {

    User loginUser = userService.getLoginOrNull();
    Boolean likedByUser = (loginUser != null)
        ? review.getLikes().stream().anyMatch(like -> like.getUser().equals(loginUser))
        : null;

    int commentCount = review.getComments().size();

    Movie movie = review.getMovie();
    ReviewResponseDTO.ReviewResponseDTOBuilder builder = ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .nickname(review.getUser().getNickname())
        .profileImageUrl(review.getUser().getProfileImageUrl())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .commentCount(commentCount)
        .likeCount(review.getLikeCount())
        .likedByUser(likedByUser)
        .tmdbId(movie.getTmdbId())
        .movieTitle(movie.getTitle())
        .posterPath(movie.getPosterPath())
        .certified(review.isCertified());

    if (includeComments) {
      builder.comments(review.getComments().stream()
          .map(this::convertToCommentDto)
          .collect(Collectors.toList()));
    }
    return builder.build();
  }

  private CommentResponseDto convertToCommentDto(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId())
        .reviewId(comment.getReview().getId())
        .userId(comment.getUser().getId())
        .userNickname(comment.getUser().getNickname())
        .profileImageUrl(comment.getUser().getProfileImageUrl())
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }
}
