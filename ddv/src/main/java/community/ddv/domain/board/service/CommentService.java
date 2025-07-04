package community.ddv.domain.board.service;

import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.global.exception.ErrorCode;
import community.ddv.domain.board.dto.CommentDTO.CommentRequestDto;
import community.ddv.domain.board.dto.CommentDTO.CommentResponseDto;
import community.ddv.domain.board.entity.Comment;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.notification.NotificationService;
import community.ddv.domain.user.entity.User;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.domain.board.repository.CommentRepository;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.response.CursorPageResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

  private final UserService userService;
  private final ReviewService reviewService;
  private final forbiddenWordsFilter forbiddenWordsFilter;
  private final CommentRepository commentRepository;
  private final ReviewRepository reviewRepository;
  private final NotificationService notificationService;

  @Transactional
  public CommentResponseDto createComment(Long reviewId, CommentRequestDto commentRequestDto) {

    User user = userService.getLoginUser();
    log.info("[CREATE_COMMENT] 댓글 작성 시도 - reviewId = {}, userId = {} ", reviewId, user.getId());

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> {
          log.warn("[CREATE_COMMENT] 존재하지 않는 리뷰 - reviewId = {}", reviewId);
          return new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND);
        });

    String filteredContent = forbiddenWordsFilter.filterForbiddenWords(commentRequestDto.getContent());

    Comment comment = Comment.builder()
        .review(review)
        .user(user)
        .content(filteredContent)
        .build();

    Comment newComment = commentRepository.save(comment);
    log.info("[CREATE_COMMENT] 댓글 작성 완료 - commentId = {}, reviewId = {}, userId = {}", newComment.getId(), reviewId, user.getId());

    notificationService.commentAdded(user.getId(), reviewId);

    return convertToCommentResponse(newComment);
  }


  @Transactional
  public CommentResponseDto updateComment(Long reviewId, Long commentId, CommentRequestDto commentRequestDto) {

    User user = userService.getLoginUser();
    log.info("[UPDATE_COMMENT] 댓글 수정 시도 - commentId = {}, reviewId = {}, userId = {}", commentId, reviewId, user.getId());

    reviewRepository.findById(reviewId)
        .orElseThrow(() -> {
          log.warn("[UPDATE_COMMENT] 존재하지 않는 리뷰 - reviewId = {}", reviewId);
          return new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND);
        });

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> {
          log.warn("[UPDATE_COMMENT] 존재하지 않는 댓글 - commentId = {}", commentId);
          return new DeepdiviewException(ErrorCode.COMMENT_NOT_FOUND);
        });

    // 댓글 작성자만 수정 가능
    if (!comment.getUser().getId().equals(user.getId())) {
      log.warn("[UPDATE_COMMENT] 댓글 수정은 작성자만 수정 가능합니다. 작성자 userId = {}, 수정 요청자 userId = {}", comment.getUser().getId(), user.getId());
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    String filteredContent = forbiddenWordsFilter.filterForbiddenWords(commentRequestDto.getContent());


    comment.updateContent(filteredContent);
    commentRepository.flush();
    log.info("[UPDATE_COMMENT] 댓글 수정 완료 - commentId = {}", commentId);
    return convertToCommentResponse(comment);
  }


  @Transactional
  public void deleteComment(Long reviewId, Long commentId) {

    User user = userService.getLoginUser();
    log.info("[DELETE_COMMENT] 댓글 삭제 시도 - commentId = {}, reviewId = {}, userId = {}", commentId, reviewId, user.getId());

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> {
          log.warn("[DELETE_COMMENT] 존재하지 않는 리뷰 - reviewId = {}", reviewId);
          return new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND);
        });

    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> {
          log.warn("[DELETE_COMMENT] 존재하지 않는 댓글 - commentId = {}", commentId);
          return new DeepdiviewException(ErrorCode.COMMENT_NOT_FOUND);
        });

    // 댓글 작성자만 삭제 가능
    if (!comment.getUser().getId().equals(user.getId())) {
      log.warn("[DELETE_COMMENT] 댓글 삭제는 작성자만 삭제 가능합니다. - 작성자 userId = {}, 삭제 요청자 userId = {}", comment.getUser().getId(), user.getId());
      throw new DeepdiviewException(ErrorCode.INVALID_USER);
    }

    if (!comment.getReview().getId().equals(review.getId())) {
      throw new DeepdiviewException(ErrorCode.COMMENT_NOT_BELONG_TO_REVIEW);
    }

    commentRepository.delete(comment);
    log.info("[DELETE_COMMENT] 댓글 삭제 완료 - commentId = {}", commentId);

  }


  @Transactional(readOnly = true)
  public CursorPageResponse<CommentResponseDto> getCommentsByReviewId(
      Long reviewId, LocalDateTime cursorCreatedAt, Long cursorId, int size) {
    //log.info("[COMMENT] 리뷰 {}의 댓글 조회 요청", reviewId);

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    // 커서 방식이므로 Page 번호는 항상 0으로 고정
    // size + 1로 요청한 이유 : 다음 페이지가 존재하는지 판단하기 위해 하나 더 가져옴
    Pageable pageable = PageRequest.of(0, size+1);

    List<Comment> comments;
    // 커서 정보가 있는 경우 이전 커서보다 더 이전 데이터 조회
    if (cursorCreatedAt != null && cursorId != null) {
      comments = commentRepository.findCommentsByReviewBeforeCursor(review, cursorCreatedAt, cursorId, pageable);
    } else {
      // 커서 정보가 없는 경우 (첫 요청) 최신순 댓글 가져옴
      comments = commentRepository.findByReviewOrderByCreatedAtDescIdDesc(review, pageable);
    }

    // 아직 더 가져올 데이터가 있는지 여부
    boolean hasNext = comments.size() > size;

    // 응답으로는 요청한 size만 보내야 하니까 일부러 더 넣은 1개 제거
    if (hasNext) {
      comments = comments.subList(0, size);
    }

    List<CommentResponseDto> content = comments.stream()
        .map(this::convertToCommentResponse)
        .toList();

    // 다음 커서로 사용할 createdAt 및 id 세팅
    LocalDateTime nextCreatedAt = null;
    Long nextId = null;

    if (hasNext && !comments.isEmpty()) {
      Comment last = comments.get(comments.size() - 1);
      nextCreatedAt = last.getCreatedAt();
      nextId = last.getId();
    }

    return new CursorPageResponse<>(content, nextCreatedAt, nextId, hasNext);
  }


  @Transactional(readOnly = true)
  public Page<CommentResponseDto> getCommentsByUserId(Long userId, Pageable pageable) {
    log.info("[COMMENT] userId = {}가 작성한 댓글 조회 요청", userId);
    userService.getLoginUser();

    return commentRepository.findByUser_Id(userId, pageable)
        .map(this::convertToCommentResponseByUser);
  }


  private CommentResponseDto convertToCommentResponse(Comment comment) {
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

  private CommentResponseDto convertToCommentResponseByUser(Comment comment) {

    ReviewResponseDTO reviewDto = reviewService.convertToReviewResponseDto(comment.getReview());
    return CommentResponseDto.builder()
        .id(comment.getId())
        .reviewId(comment.getReview().getId())
        .userId(comment.getUser().getId())
        .userNickname(comment.getUser().getNickname())
        .profileImageUrl(comment.getUser().getProfileImageUrl())
        .content(comment.getContent())
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .review(reviewDto)
        .build();
  }
}
