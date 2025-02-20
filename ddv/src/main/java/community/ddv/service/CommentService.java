package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.CommentDTO.CommentRequestDto;
import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.entity.Comment;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.CommentRepository;
import community.ddv.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final UserService userService;
  private final CommentRepository commentRepository;
  private final ReviewRepository reviewRepository;
  private final NotificationService notificationService;

  @Transactional
  public CommentResponseDto createComment(Long reviewId, CommentRequestDto commentRequestDto) {

    log.info("댓글 작성 요청");
    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));
    log.info("댓글이 달릴 reviewId: {}", review.getId());

    Comment comment = Comment.builder()
        .review(review)
        .user(user)
        .content(commentRequestDto.getContent())
        .build();

    Comment newComment = commentRepository.save(comment);
    log.info("댓글 작성 완료");
    return convertToCommentResponse(newComment);
  }


  @Transactional
  public CommentResponseDto updateComment(Long reviewId, Long commentId, CommentRequestDto commentRequestDto) {

    log.info("댓글 수정 요청");
    User user = userService.getLoginUser();

    reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.COMMENT_NOT_FOUND));

    if (!comment.getUser().getId().equals(user.getId())) {
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    comment.updateContent(commentRequestDto.getContent());
    log.info("댓글 수정 완료");

    notificationService.commentAdded(user.getId(), reviewId);
    return convertToCommentResponse(comment);
  }


  @Transactional
  public void deleteComment(Long reviewId, Long commentId) {
    log.info("댓글 삭제 요청");

    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.COMMENT_NOT_FOUND));

    if (!comment.getUser().getId().equals(user.getId())) {
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    if (!comment.getReview().getId().equals(review.getId())) {
      throw new DeepdiviewException(ErrorCode.NOT_MATCHED_CONTENT);
    }

    log.info("댓글 삭제 완료");
    commentRepository.delete(comment);
  }

  @Transactional(readOnly = true)
  public Page<CommentResponseDto> getCommentsByReviewId(Long reviewId, Pageable pageable) {
    log.info("댓글 조회 요청");
    userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Page<Comment> comments = commentRepository.findByReview(review, pageable);
    log.info("댓글 조회 성공");

    return comments.map(this::convertToCommentResponse);
  }

  @Transactional(readOnly = true)
  public Page<CommentResponseDto> getCommentsByUserId(Long userId, Pageable pageable) {
    log.info("특정 이용자가 작성한 댓글 조회 요청");
    userService.getLoginUser();

    Page<Comment> comments = commentRepository.findByUser_Id(userId, pageable);
    log.info("특정 이용자가 작성한 댓글 조회 성공");

    return comments.map(this::convertToCommentResponse);
  }


  private CommentResponseDto convertToCommentResponse(Comment comment) {
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
