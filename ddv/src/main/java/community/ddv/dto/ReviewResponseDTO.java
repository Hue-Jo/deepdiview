package community.ddv.dto;

import community.ddv.dto.CommentDTO.CommentResponseDto;
import java.time.LocalDateTime;
import java.util.List;
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
//  private Long movieId;
//  private Long tmdbId;
//  private String movieTitle;
  private String reviewTitle;
  private String reviewContent;
  private int rating;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private List<CommentResponseDto> comments;
}
