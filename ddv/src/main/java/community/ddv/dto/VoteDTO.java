package community.ddv.dto;

import community.ddv.entity.Movie;
import community.ddv.entity.Vote;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
public class VoteDTO {

  @Getter
  @NoArgsConstructor
  public static class VoteRequestDto {
    private String title; // 투표 제목

  }

  @Getter
  public static class VoteResponseDTO {
    private Long voteId;
    private String title; // 투표 제목
    private List<Long> movieTmdbIds; // 투표에 포함된 영화 리스트
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public VoteResponseDTO(Vote vote) {
      voteId = vote.getId();
      title = vote.getTitle();
      startDate = vote.getStartDate();
      endDate = vote.getEndDate();
      movieTmdbIds = vote.getMovies().stream()
          .map(Movie::getTmdbId)
          .collect(Collectors.toList());
    }
  }

}
