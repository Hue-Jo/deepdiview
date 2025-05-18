package community.ddv.domain.board.dto;

import community.ddv.domain.board.custom.RatingValid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewDTO {

  private Long tmdbId;

  @NotBlank(message = "제목을 작성해주세요")
  private String title;

  @NotBlank(message = "내용을 작성해주세요")
  private String content;

  @RatingValid
  private Double rating;

  private boolean isCertified;

  @Getter
  @AllArgsConstructor
  public static class ReviewUpdateDTO {

    private String title;
    private String content;

    @RatingValid
    private Double rating;

  }

}
