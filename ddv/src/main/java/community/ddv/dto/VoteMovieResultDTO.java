package community.ddv.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class VoteMovieResultDTO {

  private Long tmdbId;
  private int voteCount; // 득표수
  private int rank;      // 등수
  private LocalDateTime lastVotedTime; // 마지막 득표 시간

  public VoteMovieResultDTO(Long tmdbId, int voteCount) {
    this.tmdbId = tmdbId;
    this.voteCount = voteCount;
  }



}
