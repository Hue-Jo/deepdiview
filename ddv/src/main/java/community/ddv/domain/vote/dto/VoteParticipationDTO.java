package community.ddv.domain.vote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class VoteParticipationDTO {

  @Getter
  public static class VoteParticipationRequestDto {
    @NotNull(message = "투표할 영화를 선택해주세요")
    private Long tmdbId; // 사용자가 선택한 영화
  }


}
