package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.constant.Role;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.VoteDTO.VoteRequestDto;
import community.ddv.dto.VoteDTO.VoteResponseDTO;
import community.ddv.entity.Movie;
import community.ddv.entity.User;
import community.ddv.entity.Vote;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.UserRepository;
import community.ddv.repository.VoteRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoteService {

  private final VoteRepository voteRepository;
  private final UserRepository userRepository;
  private final MovieRepository movieRepository;
  private final UserService userService;
  private final MovieService movieService;

  /**
   * 관리자의 투표 생성
   * @param voteRequestDTO
   */
  @Transactional
  public VoteResponseDTO createVote(VoteRequestDto voteRequestDTO) {

    // 로그인된 관리자만 투표 생성 가능
    User admin = userService.getLoginUser();
    log.info("투표 생성 시도");
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 투표를 생성할 수 있습니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    // 투표 생성은 일요일,월요일만 가능
    LocalDateTime today = LocalDateTime.now();
    if (today.getDayOfWeek() != DayOfWeek.THURSDAY && today.getDayOfWeek() != DayOfWeek.MONDAY) {
      log.error("투표 생성은 일요일, 월요일만 가능합니다.");
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_CREAT_DATE);
    }

    // 투표 종료일 : 토요일 23시 59분 59초
    LocalDateTime endDate = today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
        .withHour(23).withMinute(23).withSecond(59);

    // 인기도 탑 5의 영화 세부 정보 가져오기
    List<MovieDTO> top5Movies = movieService.getTop5Movies();
    log.info("인기도 탑5의 영화를 가져왔습니다.");

    // 인기도 탑5 영화 목록에서 Movie 객체를 리스트로 변환
    List<Movie> selectedMovies = top5Movies.stream()
        .map(movieDTO -> movieRepository.findByTmdbId(movieDTO.getId())
            .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND)))
        .collect(Collectors.toList());

    Vote vote = Vote.builder()
        .title(voteRequestDTO.getTitle())
        .movies(selectedMovies)
        .startDate(today)
        .endDate(endDate)
        .build();
    Vote savedVote = voteRepository.save(vote);
    log.info("투표 생성 완료");
    return new VoteResponseDTO(savedVote);

  }

  // 일반 사용자의 투표 여부 확인
  public boolean hasVoted(Long userId) {
    User user = userService.getLoginUser();
    userRepository.findById(userId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));
    return voteRepository.existsByUser(user);
  }

}
