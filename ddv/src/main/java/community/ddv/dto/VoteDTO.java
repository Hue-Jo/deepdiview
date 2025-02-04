package community.ddv.dto;

import community.ddv.entity.Movie;
import community.ddv.entity.Vote;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 투표 생성 & 결과 관련 DTO
@Getter
public class VoteDTO {

  @Getter
  public static class VoteCreatedDTO {

    private Long voteId;
    private String title; // 투표 제목
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    //private List<Long> movieTmdbIds; // 투표에 포함된 영화 리스트
    private List<MovieVoteDto> movieDetails = new ArrayList<>();  // tmdbIds, 등수, 투표수,


    public VoteCreatedDTO(Vote vote) {
      voteId = vote.getId();
      title = vote.getTitle();
      startDate = vote.getStartDate();
      endDate = vote.getEndDate();

      int rank = 1;
      for (Movie movie : vote.getMovies()) {
        MovieVoteDto movieVoteDto = new MovieVoteDto(
            movie.getTmdbId(),
            0,
            rank
        );
        movieDetails.add(movieVoteDto);
        rank++;

      }
//      movieTmdbIds = vote.getMovies().stream()
//          .map(Movie::getTmdbId)
//          .collect(Collectors.toList());
    }
  }

  @Getter
  @AllArgsConstructor
  public static class VoteResultDTO {

    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<VoteCount> results;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteCount {

      private String movieTitle;
      private Long voteCount;
      private LocalDateTime latestVoteTime; // 마지막 득표 시간
    }

  }
}