package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.entity.Comment;
import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.ReviewRepository;
import java.time.LocalDateTime;
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
    log.info("리뷰 작성 시도, 유저: {}");

    Movie movie = movieRepository.findByTmdbId(reviewDTO.getTmdbId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    if (reviewRepository.existsByUserAndMovie(user, movie)) {
      log.warn("한 영화에는 하나의 리뷰만 작성할 수 있습니다.");
      throw new DeepdiviewException(ErrorCode.ALREADY_COMMITED_REVIEW);
    }

    Review review = Review.builder()
        .user(user)
        .movie(movie)
        .tmdbId(movie.getTmdbId())
        .title(reviewDTO.getTitle())
        .content(reviewDTO.getContent())
        .rating(reviewDTO.getRating())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    Review savedReview = reviewRepository.save(review);
    log.info("리뷰 작성 성공");
    return convertToResponseDto(savedReview);
  }

  /**
   * 리뷰 삭제 _ 작성자만 가능
   * @param reviewId
   */
  @Transactional
  public void deleteReview(Long reviewId) {

    User user = userService.getLoginUser();
    log.info("리뷰 삭제 시도");

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().equals(user)) {
      log.warn("리뷰 삭제 권한 없음");
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }
    reviewRepository.delete(review);
    log.info("리뷰 삭제 완료");
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
    log.info("리뷰 수정 시도");

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getUser().equals(user)) {
      log.warn("리뷰 수정 권한 없음");
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    if (!reviewUpdateDTO.getTitle().isEmpty()) {
      review.setTitle(reviewUpdateDTO.getTitle());
    }
    if (!reviewUpdateDTO.getContent().isEmpty()) {
      review.setContent(reviewUpdateDTO.getContent());
    }
    if (reviewUpdateDTO.getRating() != null) {
      review.setRating(reviewUpdateDTO.getRating());
    }

    review.setUpdatedAt(LocalDateTime.now());

    Review updatedReview = reviewRepository.save(review);
    log.info("리뷰 수정 완료");

    return convertToResponseDto(updatedReview);
  }

  /**
   * 특정 영화의 리뷰 조회
   * @param tmdbId
   *
   */
  @Transactional(readOnly = true)
  public Page<ReviewResponseDTO> getReviewByMovieId(Long tmdbId, Pageable pageable) {
    log.info("특정 영화의 리뷰 조회 시도");
    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    Page<Review> reviews = reviewRepository.findByMovie(movie, pageable);

    log.info("특정 영화의 리뷰 조회 완료");
    return reviews.map(this::convertToResponseDto);
  }

  /**
   * 특정 리뷰 조회
   * @param reviewId
   */
  @Transactional(readOnly = true)
  public ReviewResponseDTO getReviewById(Long reviewId) {
    log.info("특정 리뷰 조회 요청");

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    log.info("특정 리뷰 조회 성공");
    return convertToResponseWithCommentsDto(review);
  }

  /**
   * 특정 사용자가 작성한 리뷰 조회
   * @param userId
   * @return
   */
  @Transactional(readOnly = true)
  public Page<ReviewResponseDTO> getReviewsByUserId(Long userId, Pageable pageable) {
    log.info("특정 사용자의 리뷰 내역 조회 요청");
    userService.getLoginUser();

    Page<Review> reviews = reviewRepository.findByUser_Id(userId, pageable);
    log.info("특정 사용자의 리뷰 내역 조회 성공");

    return reviews.map(this::convertToResponseDto);

  }


  private ReviewResponseDTO convertToResponseDto(Review review) {
    return ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .nickname(review.getUser().getNickname())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .likeCount(review.getLikeCount())
        .build();
  }

  private ReviewResponseDTO convertToResponseWithCommentsDto(Review review) {
    return ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .nickname(review.getUser().getNickname())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .likeCount(review.getLikeCount())
        .comments(review.getComments().stream()
            .map(this::convertToCommentDto)
            .collect(Collectors.toList()))
        .build();
  }

  private CommentResponseDto convertToCommentDto(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId())
        .reviewId(comment.getReview().getId())
        .userId(comment.getUser().getId())
        .userNickname(comment.getUser().getNickname())
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }
}
