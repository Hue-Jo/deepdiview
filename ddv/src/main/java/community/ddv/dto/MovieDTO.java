package community.ddv.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class MovieDTO {

  private Long id;       // tmdb에서 제공하는 id
  private String title;           // 제목
  private String original_title;  // 원어 제목
  private String overview;        // 즐거리
  private LocalDate release_date; // 개봉일
  private Double popularity;      // 인기도
  private String poster_path;     // 포스터 url
  private String backdrop_path;   // 백드롭이미지 url
  private List<Long> genre_ids;   // 장르 아이디 리스트

}
