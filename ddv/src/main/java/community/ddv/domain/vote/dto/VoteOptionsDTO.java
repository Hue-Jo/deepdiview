package community.ddv.domain.vote.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoteOptionsDTO {

  private List<Long> tmdbIds; // 5개 선택지

}
