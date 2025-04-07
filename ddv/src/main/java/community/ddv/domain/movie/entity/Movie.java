package community.ddv.domain.movie.entity;

import community.ddv.domain.board.entity.Review;
import community.ddv.domain.vote.entity.VoteMovie;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Movie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 본 서비스에서 사용하는 id

  @Column(unique = true, nullable = false)
  private Long tmdbId;       // tmdb에서 제공하는 id
  private String title;          // 제목
  private String originalTitle;  // 원어 제목

  @Column(columnDefinition = "TEXT")
  private String overview;       // 즐거리

  private LocalDate releaseDate; // 개봉일
  @Setter
  private Double popularity;     // 인기도
  private String posterPath;     // 포스터 url
  private String backdropPath;   // 백드롭이미지 url

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL)
  private final Set<MovieGenre> movieGenres = new HashSet<>();

  public void addGenre(Genre genre) {
    MovieGenre movieGenre = new MovieGenre(this, genre);
    movieGenres.add(movieGenre);
  }

  @Setter
  @Builder.Default
  private boolean isAvailable = true; // 넷플에서 제공중인지 여부 (기본값 true)

  public void changeAsUnavailable() {
    this.isAvailable = false;
  }

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL)
  @Builder.Default
  private List<VoteMovie> voteMovies = new ArrayList<>();

  @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL)
  @Builder.Default
  private List<Review> reviews = new ArrayList<>();
}
