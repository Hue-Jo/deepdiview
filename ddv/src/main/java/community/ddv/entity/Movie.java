package community.ddv.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Movie {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 본 서비스에서 사용하는 id

  private Long timdbId;       // tmdb에서 제공하는 id
  private String title;          // 제목
  private String originalTitle;  // 원어 제목

  @Column(columnDefinition = "TEXT")
  private String overview;       // 즐거리

  private LocalDate releaseDate; // 개봉일
  private Double popularity;     // 인기도
  private String posterPath;     // 포스터 url
  private String backdropPath;   // 백드롭이미지 url

}
