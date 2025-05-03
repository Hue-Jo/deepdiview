package community.ddv.domain.movie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate release_date; // 개봉일
  private Double popularity;      // 인기도
  private String poster_path;     // 포스터 url
  private String backdrop_path;   // 백드롭이미지 url
  private List<Long> genre_ids;   // 장르 아이디 리스트
  private List<String> genre_names; // 장르 이름 리스트

  private List<ReviewResponseDTO> reviews;
  private ReviewResponseDTO myReview;
  private Double ratingAverage;

  private boolean isAvailable; // 현재 제공 중인지 여부

  private Map<Double, Integer> ratingDistribution; // 별점 분포

}
