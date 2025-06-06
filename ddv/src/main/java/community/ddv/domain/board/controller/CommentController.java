package community.ddv.domain.board.controller;

import community.ddv.domain.board.dto.CommentDTO;
import community.ddv.domain.board.dto.CommentDTO.CommentResponseDto;
import community.ddv.domain.board.service.CommentService;
import community.ddv.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews/{reviewId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API에 대한 명세를 제공합니다.")
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 작성", description = "특정 리뷰에 대해 여러 댓글을 작성할 수 있습니다.")
  @PostMapping
  public ResponseEntity<CommentResponseDto> createComment(
      @PathVariable Long reviewId,
      @Valid @RequestBody CommentDTO.CommentRequestDto commentRequestDto) {

    CommentResponseDto comment = commentService.createComment(reviewId,
        commentRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(comment);

  }

  @Operation(summary = "댓글 수정", description = "리뷰 id와 댓글 id가 정확히 매칭되어야 수정됩니다. 댓글 작성자만 수정 가능")
  @PutMapping("/{commentId}")
  public ResponseEntity<CommentResponseDto> updateComment(
      @PathVariable Long reviewId,
      @PathVariable Long commentId,
      @Valid @RequestBody CommentDTO.CommentRequestDto commentRequestDto
  ) {
    CommentResponseDto commentResponseDto = commentService.updateComment(reviewId, commentId,
        commentRequestDto);
    return ResponseEntity.ok(commentResponseDto);
  }

  @Operation(summary = "댓글 삭제", description = "리뷰 id와 댓글 id가 정확히 매칭되어야 삭제됩니다. 댓글 작성자만 삭제 가능")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable Long reviewId,
      @PathVariable Long commentId
  ) {
    commentService.deleteComment(reviewId, commentId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "특정 리뷰에 달린 댓글 조회")
  @GetMapping
  public ResponseEntity<CursorPageResponse<CommentResponseDto>> getCommentsByReviewId(
      @PathVariable Long reviewId,
      @RequestParam(required = false) LocalDateTime createdAt,
      @RequestParam(required = false) Long commentId,
      @RequestParam(defaultValue = "20") int size) {
    CursorPageResponse<CommentResponseDto> response =
        commentService.getCommentsByReviewId(reviewId, createdAt, commentId, size);
    return ResponseEntity.ok(response);
  }
}