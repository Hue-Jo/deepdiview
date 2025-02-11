package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.entity.Comment;
import community.ddv.entity.Movie;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.ReviewRepository;
import community.ddv.repository.UserRepository;
import community.ddv.dto.ReviewResponseDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final MovieRepository movieRepository;

  /**
   * 영화 리뷰 작성 _ 유저는 특정 영화에 대해 한 번만 리뷰 작성 가능
   * @param reviewDTO
   */
  @Transactional
  public ReviewResponseDTO createReview(String email, ReviewDTO reviewDTO) {
    log.info("리뷰 작성 시도, 유저: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

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
  public void deleteReview(String email, Long reviewId) {
    log.info("리뷰 삭제 시도, 유저: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

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
  public ReviewResponseDTO updateReview(String email, Long reviewId, ReviewUpdateDTO reviewUpdateDTO) {
    log.info("리뷰 수정 시도, 유저: {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

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

  @Transactional(readOnly = true)
  public List<ReviewResponseDTO> getReviewByMovieId(Long tmdbId) {
    log.info("특정 영화의 리뷰 조회 시도");
    Movie movie = movieRepository.findByTmdbId(tmdbId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    List<Review> reviews = reviewRepository.findByMovie(movie);

    log.info("특정 영화의 리뷰 조회 완료");
    return reviews.stream()
        .map(this::convertToResponseDto)
        .collect(Collectors.toList());
  }

  private ReviewResponseDTO convertToResponseDto(Review review) {
    return ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .build();
  }

  private ReviewResponseDTO convertToResponseWithCommentsDto(Review review) {
    return ReviewResponseDTO.builder()
        .reviewId(review.getId())
        .userId(review.getUser().getId())
        .reviewTitle(review.getTitle())
        .reviewContent(review.getContent())
        .rating(review.getRating())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .comments(review.getComments().stream()
            .map(this::convertToCommentDto)
            .collect(Collectors.toList()))
        .build();
  }

  private CommentResponseDto convertToCommentDto(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId())
        .reviewId(comment.getReview().getId())
        .content(comment.getContent())
        .userNickname(comment.getUser().getNickname())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }
}
