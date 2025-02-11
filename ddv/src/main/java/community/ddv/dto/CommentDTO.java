package community.ddv.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String userNickname;
    private String content;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }


}
