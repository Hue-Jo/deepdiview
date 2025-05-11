package community.ddv.domain.board.entity;

import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Review {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_id")
  private Movie movie;

  private Long tmdbId;

  private String title; // 제목

  @Column(length = 1000)
  private String content; // 내용 (1000자 이내로 제한)

  private Double rating; // 별점

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime updatedAt;

  @Column(nullable = false)
  private Integer likeCount;

  public void increaseLikeCount() {
    likeCount = (likeCount == null) ? 0 : likeCount + 1;
  }
  public void decreaseLikeCount() {
    likeCount = (likeCount == null || likeCount <= 0) ? 0 : likeCount - 1;
  }

  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Like> likes = new ArrayList<>();

  private boolean certified;

  public void updateCertified(boolean certified) {
    this.certified = certified;
  }

  public void updateReview(String title, String content, Double rating) {
    if (title != null && !title.isEmpty()) {
      this.title = title;
    }
    if (content != null && !content.isEmpty()) {
      this.content = content;
    }
    if (rating != null) {
      this.rating = rating;
    }
  }


  @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Comment> comments = new ArrayList<>();

}
