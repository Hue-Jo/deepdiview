package community.ddv.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class CommentDTO {

  @Getter
  public static class CommentRequestDto {
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

  }


}
