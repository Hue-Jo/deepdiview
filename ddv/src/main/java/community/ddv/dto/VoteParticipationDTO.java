package community.ddv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class VoteParticipationDTO {

  @Getter
  public static class VoteParticipationRequestDto {

    private Long tmdbId;
  }

  @Getter
  @AllArgsConstructor
  public static class VoteParticipationResponseDto {

    private boolean voteSuccess;

  }




}
