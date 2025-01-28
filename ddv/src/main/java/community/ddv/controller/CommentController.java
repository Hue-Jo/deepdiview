package community.ddv.controller;

import community.ddv.dto.CommentDTO.CommentRequestDto;
import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews/{reviewId}/comments")
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 작성", description = "특정 리뷰에 대해 여러 댓글을 작성할 수 있습니다.")
  @PostMapping
  public ResponseEntity<CommentResponseDto> createComment(
      @PathVariable Long reviewId,
      @RequestBody CommentRequestDto commentRequestDto) {

    CommentResponseDto commentResponseDto = commentService.createComment(reviewId, commentRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(commentResponseDto);


  }

}
