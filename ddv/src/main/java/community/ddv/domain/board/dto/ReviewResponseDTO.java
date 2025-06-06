package community.ddv.domain.board.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ReviewResponseDTO {

  private Long reviewId;
  private Long userId;
  private String nickname;
  private String profileImageUrl;
  private String reviewTitle;
  private String reviewContent;
  private Double rating;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private Integer commentCount;
  private Integer likeCount;
  private Boolean likedByUser;
  private Long tmdbId;
  private String movieTitle;
  private String posterPath;
  private Boolean certified;

}
