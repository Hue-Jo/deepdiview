package community.ddv.domain.vote.dto;

import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.vote.entity.Vote;
import community.ddv.domain.vote.entity.VoteMovie;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 투표 생성 & 결과 관련 DTO
@Getter
public class VoteDTO {

  @Getter
  public static class VoteCreatedDTO {

    private Long voteId;
    private String title; // 투표 제목
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<VoteMovieResultDTO> movieDetails = new ArrayList<>();  // tmdbIds, 등수, 투표수

    public VoteCreatedDTO(Vote vote) {
      voteId = vote.getId();
      title = vote.getTitle();
      startDate = vote.getStartDate();
      endDate = vote.getEndDate();

      int rank = 1;
      for (VoteMovie voteMovie : vote.getVoteMovies()) {
        Movie movie = voteMovie.getMovie();
        VoteMovieResultDTO voteMovieResultDTO = new VoteMovieResultDTO(
            movie.getTmdbId(),
            voteMovie.getVoteCount(),
            0,
            voteMovie.getLastVotedAt()
        );
        movieDetails.add(voteMovieResultDTO);
        rank++;

      }
    }
  }

  @Getter
  @AllArgsConstructor
  public static class VoteResultDTO {

    private Long voteId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean isActivating; // 진행중 여부
    private List<VoteMovieResultDTO> results; // 영화별 투표 결과 리스트

  }
}
