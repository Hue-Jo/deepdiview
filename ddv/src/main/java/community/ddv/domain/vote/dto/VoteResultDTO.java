package community.ddv.domain.vote.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 투표 결과 DTO
@Getter
@AllArgsConstructor
public class VoteResultDTO {

  // 여러 영화의 투표 결과를 모은 전체 결과 목록을 담는 DTO
  private List<VoteMovieResultDTO> results; // 영화별 투표 결과 리스트

}
