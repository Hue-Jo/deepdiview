package community.ddv.domain.board.service;

import community.ddv.global.exception.ErrorCode;
import community.ddv.domain.board.entity.Like;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.notification.NotificationService;
import community.ddv.domain.user.entity.User;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.domain.board.repository.LikeRepository;
import community.ddv.domain.user.service.UserService;
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

    boolean existingLike = likeRepository.existsByReviewAndUser(review, user);

    // 좋아요가 눌러져 있으면 좋아요 취소, 좋아요를 누른 적이 없으면 좋아요
    if (existingLike) {
      unlike(user, review);
    } else {
      like(user, review);
    }
  }

  private void like(User user, Review review) {
    Like newlike = Like.builder()
        .user(user)
        .review(review)
        .build();
    likeRepository.save(newlike);
    review.increaseLikeCount();
    log.info("좋아요 성공 (좋아요 +1)");
    notificationService.likeAdded(user.getId(), review.getId());
  }

  private void unlike(User user, Review review) {
    likeRepository.deleteByReviewAndUser(review, user);
    review.decreaseLikeCount();
    log.info("좋아요 취소 (좋아요 -1)");
  }
}
