package community.ddv.entity;

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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class VoteParticipation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vote_Id")
  private Vote vote;

//  @ManyToOne(fetch = FetchType.LAZY)
//  @JoinColumn(name = "movie_id")
//  private Movie selectedMovie;

  @ManyToOne(fetch = FetchType.LAZY)
  private VoteMovie selectedVoteMovie;

  @CreationTimestamp
  private LocalDateTime votedAt;

}