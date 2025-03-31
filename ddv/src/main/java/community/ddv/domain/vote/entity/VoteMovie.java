package community.ddv.domain.vote.entity;

import community.ddv.domain.movie.entity.Movie;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class VoteMovie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vote_id")
  private Vote vote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_id")
  private Movie movie;

  private int voteCount;
  private LocalDateTime lastVotedAt;


  public void updateLastVotedAt() {
    if (this.lastVotedAt == null) {
      this.lastVotedAt = LocalDateTime.now();  // 최초 투표 시점
    }
  }

  public void plusVoteCount() {
    this.voteCount++;
    updateLastVotedAt();  // 득표 시점 업데이트
  }
}
