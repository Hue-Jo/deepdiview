package community.ddv.domain.board.dto;

import community.ddv.domain.board.custom.RatingValid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewDTO {

  private Long tmdbId;

  @NotBlank(message = "제목을 작성해주세요")
  @Size(max = 60, message = "제목은 60자 이하로 작성해주세요")
  private String title;

  @NotBlank(message = "내용을 작성해주세요")
  @Size(max = 10000, message = "내용은 10000자 이하로 작성해주세요")
  private String content;

  @RatingValid
  private Double rating;

  private boolean isCertified;

  @Getter
  @AllArgsConstructor
  public static class ReviewUpdateDTO {

    @Size(max = 60, message = "제목은 60자 이하로 작성해주세요")
    private String title;

    @Size(max = 10000, message = "내용은 10000자 이하로 작성해주세요")
    private String content;

    @RatingValid
    private Double rating;

  }

}
