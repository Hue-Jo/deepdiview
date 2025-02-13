package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.entity.Like;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.LikeRepository;
import community.ddv.repository.ReviewRepository;
import community.ddv.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

  private final LikeRepository likeRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  @Transactional
  public void toggleLike(Long reviewId) {
    User user = userService.getLoginUser();
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Like existingLike = likeRepository.findByUserAndReview(user, review).orElse(null);
    // 좋아요가 눌러져 있으면 좋아요 취소, 좋아요를 누른 적이 없으면 좋아요
    if (existingLike != null) {
      likeRepository.delete(existingLike);
      review.setLikeCount(review.getLikeCount() - 1);
      reviewRepository.save(review);
      log.info("좋아요 취소 (좋아요 -1)");
    } else {
      Like newlike = Like.builder()
          .user(user)
          .review(review)
          .build();
      likeRepository.save(newlike);
      if (review.getLikeCount() == null) {
        review.setLikeCount(0);
      }
      review.setLikeCount(review.getLikeCount() + 1);
      reviewRepository.save(review);
      log.info("졸아요 성공 (좋아요 +1)");
    }
  }

}
