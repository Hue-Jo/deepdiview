package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.MovieDTO;
import community.ddv.entity.Movie;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

  private final MovieRepository movieRepository;

  /**
   * 넷플릭스 내 인기도 탑 20 영화 세부정보 조회
   */
  public List<MovieDTO> getTop20Movies() {
    Pageable pageable = PageRequest.of(0, 20);
    Page<Movie> top20Movies = movieRepository.findTop20ByOrderByPopularityDesc(pageable);
    return top20Movies.stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
  }


  /**
   * 영화 제목으로 해당 영화의 세부정보 조회 _ 공백 무시 가능 & 특정 글자가 포함되는 조회됨 & 넷플 인기도 순 정렬
   * @param title
   */
  public List<MovieDTO> searchMoviesByTitle(String title) {
    try {
      List<Movie> movies = movieRepository.findByTitleFlexible(title);
      if (movies.isEmpty()) {
        throw new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
      }
      return movies.stream()
          .map(this::convertToDTO)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("영화 정보 조회 중 오류 발생");
    }
  }

  /**
   * 특정 영화의 세부정보 조회 _ 공백 무시 가능
   * @param title
   */
  public MovieDTO getMovieInfoByTitle(String title) {
    Optional<Movie> movie = movieRepository.findByTitle(title);
    if (movie.isPresent()) {
      movie.ifPresent(this::convertToDTO);
    } else {
      throw new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND);
    }
    return convertToDTO(movie.get());
  }


  private MovieDTO convertToDTO(Movie movie) {

    return new MovieDTO(
        movie.getId(),
        movie.getTitle(),
        movie.getOriginalTitle(),
        movie.getOverview(),
        movie.getReleaseDate(),
        movie.getPopularity(),
        movie.getPosterPath(),
        movie.getBackdropPath(),
        movie.getMovieGenres().stream()
            .map(movieGenre -> movieGenre.getGenre().getId())
            .collect(Collectors.toList())
    );
  }
}
