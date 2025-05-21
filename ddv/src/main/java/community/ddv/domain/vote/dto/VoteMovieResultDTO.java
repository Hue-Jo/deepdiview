package community.ddv.domain.vote.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VoteMovieResultDTO {

  private Long tmdbId;
  private int voteCount; // 득표수
  private int rank;      // 등수
  private LocalDateTime lastVotedTime; // 마지막 득표 시간
  private Boolean voted; // 사용자가 투표를 했는지 유무

}