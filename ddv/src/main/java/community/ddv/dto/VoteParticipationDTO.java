package community.ddv.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class VoteParticipationDTO {

  @Getter
  @AllArgsConstructor
  public static class VoteOptionsDto {
    private List<Long> tmdbIds;
  }

  @Getter
  public static class VoteParticipationRequestDto {

    private Long tmdbId;
  }

  @Getter
  @AllArgsConstructor
  public static class VoteParticipationResponseDto {

    private boolean voteSuccess;
    private Long timdbId;

  }

}
