package community.ddv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDTO {

  private Long tmdbId;

  @NotBlank
  private String title;

  @NotBlank
  private String content;

  @Min(value = 1, message = "별점은 1점부터 줄 수 있습니다.")
  @Max(value = 5, message = "별점은 5점까지 줄 수 있습니다.")
  private int rating;

  @Getter
  @Setter
  public static class ReviewUpdateDTO {

    private String title;
    private String content;

    @Min(value = 1, message = "별점은 1점부터 줄 수 있습니다.")
    @Max(value = 5, message = "별점은 5점까지 줄 수 있습니다.")
    private Double rating;

  }

}
