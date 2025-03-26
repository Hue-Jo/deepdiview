package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.entity.Like;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.LikeRepository;
import community.ddv.repository.ReviewRepository;
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
  private final UserService userService;
  private final NotificationService notificationService;

  @Transactional
  public void toggleLike(Long reviewId) {
    User user = userService.getLoginUser(); // 좋아요 누르는 사람
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Like existingLike = likeRepository.findByUserAndReview(user, review).orElse(null);
    // 좋아요가 눌러져 있으면 좋아요 취소, 좋아요를 누른 적이 없으면 좋아요
    if (existingLike != null) {
      likeRepository.delete(existingLike);
      likeRepository.flush();
      review.decreaseLikeCount();
      reviewRepository.save(review);
      log.info("좋아요 취소 (좋아요 -1)");
    } else {
      Like newlike = Like.builder()
          .user(user)
          .review(review)
          .build();
      likeRepository.save(newlike);
      review.increaseLikeCount();
      log.info("좋아요 성공 (좋아요 +1)");

      notificationService.likeAdded(user.getId(), review.getId());
    }
  }

}
