package community.ddv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MovieVoteDto {

  private Long tmdbId;
  private int voteCount; // 득표수
  private int rank;      // 등수

}
