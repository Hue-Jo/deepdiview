package community.ddv.dto;

import community.ddv.entity.Movie;
import community.ddv.entity.Vote;
import community.ddv.entity.VoteMovie;
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
      for (Movie movie : vote.getVoteMovies().stream().map(VoteMovie::getMovie).toList()) {
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

    private Long voteId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<MovieVoteDto> results; // 영화별 투표 결과 리스트

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieResultDTO {

      private String timdbId; // tmdb id
      private int voteCount; // 해당 영화가 받은 득표수
      private int rank; // 등수
      private LocalDateTime latestVoteTime; // 마지막 득표 시간
    }

  }
}