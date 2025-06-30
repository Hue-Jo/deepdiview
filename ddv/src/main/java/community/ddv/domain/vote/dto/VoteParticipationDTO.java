package community.ddv.domain.vote.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class VoteParticipationDTO {

  @NotNull(message = "투표할 영화를 선택해주세요")
  private Long tmdbId; // 사용자가 선택한 영화


}
