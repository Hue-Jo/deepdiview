package community.ddv.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CommentDTO {

  @Getter
  public static class CommentRequestDto {

    @NotBlank(message = "댓글 내용을 작성해주세요.")
    @Size(max = 500, message = "댓글은 500자 이하로만 쓸 수 있습니다.")
    private String content;

  }

  @Getter
  @Builder
  @AllArgsConstructor
  public static class CommentResponseDto {
    private Long id;

    private Long reviewId;

    private Long userId;
    private String userNickname;
    private String profileImageUrl;

    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ReviewResponseDTO review;

  }


}
