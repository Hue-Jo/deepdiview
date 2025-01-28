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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final UserService userService;
  private final CommentRepository commentRepository;
  private final ReviewRepository reviewRepository;

  /**
   * 댓글 작성
   * @param reviewId
   * @param commentRequestDto
   */
  public CommentResponseDto createComment(Long reviewId, CommentRequestDto commentRequestDto) {

    log.info("댓글 작성 요청");
    User user = userService.getLoginUser();

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    Comment comment = Comment.builder()
        .review(review)
        .user(user)
        .content(commentRequestDto.getContent())
        .build();

    Comment newComment = commentRepository.save(comment);
    log.info("댓글 작성 완료");
    return mapToResponse(newComment);
  }


  private CommentResponseDto mapToResponse(Comment comment) {
    return CommentResponseDto.builder()
        .id(comment.getId())
        .reviewId(comment.getReview().getId())
        .userNickname(comment.getUser().getNickname())
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt()
        )
        .build();
  }
}
