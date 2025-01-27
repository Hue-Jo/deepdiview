package community.ddv.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ReviewResponse {

  private Long reviewId;
  private Long userId;
  private Long movieId;
  private Long tmdbId;
  private String movieTitle;
  private String reviewTitle;
  private String reviewContent;
  private int rating;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

}
