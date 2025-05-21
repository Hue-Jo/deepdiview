package community.ddv.domain.vote.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class VoteParticipationDTO {

  @Getter
  @AllArgsConstructor
  public static class VoteOptionsDto {
    //private Long voteId;
    private List<Long> tmdbIds; // 5개 선택지
  }


  @Getter
  public static class VoteParticipationRequestDto {
    @NotNull(message = "투표할 영화를 선택해주세요")
    private Long tmdbId; // 사용자가 선택한 영화
  }


//  @Getter
//  @AllArgsConstructor
//  public static class VoteParticipationResponseDto {
//
//    private boolean voteSuccess; // 성공여부
//    private Long tmdbId; // 선택한 영화
//
//  }

}
