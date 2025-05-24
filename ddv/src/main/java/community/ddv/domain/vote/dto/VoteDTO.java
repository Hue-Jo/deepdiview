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
        VoteMovieResultDTO voteMovieResultDTO = VoteMovieResultDTO.builder()
            .tmdbId(movie.getTmdbId())
            .voteCount(voteMovie.getVoteCount())
            .rank(0)
            .lastVotedTime(voteMovie.getLastVotedAt())
            .build();
        movieDetails.add(voteMovieResultDTO);
        rank++;

      }
    }
  }

  @Getter
  @AllArgsConstructor
  public static class VoteResultDTO {

    // 여러 영화의 투표 결과를 모은 전체 결과 목록을 담는 DTO
    private List<VoteMovieResultDTO> results; // 영화별 투표 결과 리스트

  }
}
