package community.ddv.domain.board.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class ReviewRatingDTO {
  private Double ratingAverage;
  private Map<Double, Integer> ratingDistribution;
}
