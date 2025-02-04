package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.constant.Role;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.VoteDTO.VoteCreatedDTO;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationRequestDto;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationResponseDto;
import community.ddv.entity.Movie;
import community.ddv.entity.User;
import community.ddv.entity.Vote;
import community.ddv.entity.VoteParticipation;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.VoteParticipationRepository;
import community.ddv.repository.VoteRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
  private final VoteParticipationRepository voteParticipationRepository;
  private final MovieRepository movieRepository;
  private final UserService userService;
  private final MovieService movieService;

  /**
   * 관리자의 투표 생성
   * 투표 생성은 일요일만 가능하지만, 실제 투표 시작은 월요일 0시 0분부터
   * 투표 종료일 : 해당 주 토요일 23시 59분 59초
   */
  @Transactional
  public VoteCreatedDTO createVote() {

    // 로그인된 관리자만 투표 생성 가능
    User admin = userService.getLoginUser();
    log.info("투표 생성 시도");
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 투표를 생성할 수 있습니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    // 투표 생성은 일요일만 가능
    LocalDateTime now = LocalDateTime.now();
    if (now.getDayOfWeek() != DayOfWeek.TUESDAY) {
      log.error("투표 생성은 일요일만 가능합니다.");
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_CREAT_DATE);
    }
    // 투표 시작일 : 생성 다음날(월요일) 자정(0시 0분)
    LocalDateTime startDate = now.plusDays(1).with(LocalTime.MIDNIGHT);
    // 투표 종료일 : 토요일 23시 59분 59초
    LocalDateTime endDate = now.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
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
        .title("다음주의 영화를 선택해주세요")
        .movies(selectedMovies)
        .startDate(startDate)
        .endDate(endDate)
        .build();
    Vote savedVote = voteRepository.save(vote);
    log.info("투표 생성 완료");
    return new VoteCreatedDTO(savedVote);

  }

  /**
   * 현재 진행중인 투표의 선택지 조회
   */
  @Transactional(readOnly = true)
  public VoteCreatedDTO getVoteChoices() {

    userService.getLoginUser();
    log.info("투표 선택지 조회 시도");

    // 현재 진행중인 투표 조회
    LocalDateTime today = LocalDateTime.now();
    Vote activatingVote = voteRepository.findByStartDateBeforeAndEndDateAfter(today, today)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD));
    log.info("투표 선택지 조회 완료");
    return new VoteCreatedDTO(activatingVote);
  }

  /**
   * 일반 사용자의 투표 참여
   * @param voteId
   * @param voteParticipationRequestDto
   */
  @Transactional
  public VoteParticipationResponseDto participateVote(Long voteId, VoteParticipationRequestDto voteParticipationRequestDto) {
    User user = userService.getLoginUser();
    log.info("투표 시도 : {} ", user.getId());

    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    // 중복참여 불가
    boolean alreadyParticipated = voteParticipationRepository.existsByUserAndVote(user, vote);
    if (alreadyParticipated) {
      log.error("이미 참여한 사용자");
      throw new DeepdiviewException(ErrorCode.AlREADY_VOTED);
    }

    // 투표 기간 내에만 참여가능
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(vote.getEndDate())) {
      log.error("이미 종료된 투표입니다.");
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD_ENDED);
    }
    if (now.isBefore(vote.getStartDate())) {
      log.error("아직 시작되지 않은 투표입니다.");
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD_NOT_STARTED);
    }

    // 영화 선택
    Movie selectedMovie = movieRepository.findByTmdbId(voteParticipationRequestDto.getTmdbId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));

    // 투표 저장
    VoteParticipation voteParticipation = VoteParticipation.builder()
        .user(user)
        .vote(vote)
        .selectedMovie(selectedMovie)
        .build();
    voteParticipationRepository.save(voteParticipation);

    log.info("투표 참여 완료: 사용자 ID = {}, 영화 ID = {}", user.getId(), selectedMovie.getId());
    return new VoteParticipationResponseDto(true);

    //return getVoteResult(vote.getId());
  }

  // 투표 결과 조회 (투표가 진행중인 경우 실시간으로 반환, 종료된 경우에는 최종결과 반환)
}
