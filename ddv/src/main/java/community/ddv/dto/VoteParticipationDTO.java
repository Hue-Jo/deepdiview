package community.ddv.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class VoteParticipationDTO {

  @Getter
  @AllArgsConstructor
  public static class VoteOptionsDto {
    private List<Long> tmdbIds; // 5개 선택지
  }

  @Getter
  public static class VoteParticipationRequestDto {

    private Long tmdbId; // 사용자가 선택한 영화
  }

  @Getter
  @AllArgsConstructor
  public static class VoteParticipationResponseDto {

    private boolean voteSuccess; // 성공여부
    private Long timdbId; // 선택한 영화

  }

}
