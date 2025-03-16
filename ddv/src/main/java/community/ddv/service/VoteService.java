package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.constant.Role;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.VoteDTO.VoteCreatedDTO;
import community.ddv.dto.VoteDTO.VoteResultDTO;
import community.ddv.dto.VoteMovieResultDTO;
import community.ddv.dto.VoteParticipationDTO.VoteOptionsDto;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationRequestDto;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationResponseDto;
import community.ddv.entity.Movie;
import community.ddv.entity.User;
import community.ddv.entity.Vote;
import community.ddv.entity.VoteMovie;
import community.ddv.entity.VoteParticipation;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.VoteParticipationRepository;
import community.ddv.repository.VoteRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
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
   * 관리자의 투표 생성 한 주에는 한 번만 투표 생성 가능
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

    // 투표 생성은 일요일만 가능, 한 주에 한 번만 가능
    LocalDateTime now = LocalDateTime.now();
    if (now.getDayOfWeek() != DayOfWeek.SUNDAY) {
      log.error("투표 생성은 일요일만 가능합니다 : 현재요일 = {}", now.getDayOfWeek());
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_CREAT_DATE);
    }

    // 이번주에 이미 생성된 투표가 있는지 확인
    LocalDateTime weekStart = now.with(DayOfWeek.SUNDAY).with(LocalTime.MIN);
    LocalDateTime weekEnd = now.with(DayOfWeek.SATURDAY).with(LocalTime.MAX);
    boolean voteAlreadyExists = voteRepository.existsByStartDateBetween(weekStart, weekEnd);
    if (voteAlreadyExists) {
      log.error("이번주에 이미 생성한 투표가 있습니다.");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_VOTE);
    }

    // 투표 시작일 : 생성 다음날(월요일) 자정(0시 0분)
    //LocalDateTime startDate = now.plusDays(1).with(LocalTime.MIDNIGHT);
    // 투표 종료일 : 토요일 23시 59분 59초
    //LocalDateTime endDate = now.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
    //    .withHour(23).withMinute(59).withSecond(59);
    // 테스트용
    LocalDateTime endDate = now.plusMinutes(3);
    log.info("테스트를 위해 3분으로 투표 종료 시간 조절");


    // 인기도 탑 5의 영화 세부 정보 가져오기
    List<MovieDTO> top5Movies = movieService.getTop5Movies();
    log.info("인기도 탑5의 영화를 가져왔습니다.");

    Vote vote = Vote.builder()
        .title("다음주의 영화를 선택해주세요")

        // 테스트용
        .startDate(now)

        //.startDate(startDate)
        .endDate(endDate)
        .voteMovies(new ArrayList<>())
        .build();

    // 선택된 영화들을 VoteMovie 테이블에 저장
    for (MovieDTO movieDTO : top5Movies) {
      Movie movie = movieRepository.findByTmdbId(movieDTO.getId())
          .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));
      VoteMovie voteMovie = VoteMovie.builder()
          .vote(vote)
          .movie(movie)
          .build();
      vote.getVoteMovies().add(voteMovie);
    }
    Vote savedVote = voteRepository.save(vote);

    log.info("투표 생성 완료 : voteId = {}, 시작시간 = {}, 종료시간 = {}", savedVote.getId(), savedVote.getStartDate(), savedVote.getEndDate());
    return new VoteCreatedDTO(savedVote);

  }

  /**
   * 현재 진행중인 투표의 선택지 조회 tmdbIds 반환
   */
  @Transactional(readOnly = true)
  public VoteOptionsDto getVoteChoices() {

    userService.getLoginUser();
    log.info("투표 선택지 조회 시도");

    // 현재 진행중인 투표 조회
    LocalDateTime today = LocalDateTime.now();
    log.debug("현재 시간 : {}", today);
    Vote activatingVote = voteRepository.findByStartDateBeforeAndEndDateAfter(today, today)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD));

    // 선택지의 tmdbId 추출
    List<Long> tmdbIds = activatingVote.getVoteMovies().stream().map(
            voteMovie -> voteMovie.getMovie().getTmdbId())
        .collect(Collectors.toList());

    log.info("투표 선택지 조회 완료");
    return new VoteOptionsDto(activatingVote.getId(), tmdbIds);
  }

  /**
   * 일반 사용자의 투표 참여
   * 중복 참여 불가
   * @param voteId
   * @param voteParticipationRequestDto
   */
  @Transactional
  public VoteParticipationResponseDto participateVote(Long voteId,
      VoteParticipationRequestDto voteParticipationRequestDto) {
    User user = userService.getLoginUser();
    log.info("투표 시도 : userId = {}, voteId = {} ", user.getId(), voteId);

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
      log.error("이미 종료된 투표입니다. voteId = {}, endDate = {}", voteId, vote.getEndDate());
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD_ENDED);
    }
    if (now.isBefore(vote.getStartDate())) {
      log.error("아직 시작되지 않은 투표입니다. voteId = {}, startDate = {}", voteId, vote.getStartDate());
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD_NOT_STARTED);
    }

    // 영화 선택
    VoteMovie selectedVotedMovie = vote.getVoteMovies().stream()
        .filter(voteMovie -> voteMovie.getMovie().getTmdbId()
            .equals(voteParticipationRequestDto.getTmdbId()))
        .findFirst()
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    // 투표 저장
    VoteParticipation voteParticipation = VoteParticipation.builder()
        .user(user)
        .vote(vote)
        .selectedVoteMovie(selectedVotedMovie)
        .build();
    voteParticipationRepository.save(voteParticipation);
    selectedVotedMovie.plusVoteCount(); // 득표수 증가 & 최종 득표시간 업데이트

    log.info("투표 참여 완료: 사용자 ID = {}, 영화 ID = {}", user.getId(),
        selectedVotedMovie.getMovie().getTmdbId());
    return new VoteParticipationResponseDto(true, selectedVotedMovie.getMovie().getTmdbId());

  }

  /**
   * 특정 투표 결과 조회
   * @param voteId
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getVoteResult(Long voteId){
    log.info("투표 결과 조회 시도 : voteId = {}", voteId);

    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> {
          log.error("투표를 찾을 수 없음 : voteId = {}", voteId);
          return new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND);
        });

    // 투표 결과 저장 리스트 생성
    List<VoteMovieResultDTO> voteResults = new ArrayList<>();
    for (VoteMovie voteMovie : vote.getVoteMovies()) {
      Movie movie = voteMovie.getMovie();
      VoteMovieResultDTO movieResultDTO = new VoteMovieResultDTO(
          movie.getTmdbId(),
          voteMovie.getVoteCount(),
          0,
          voteMovie.getLastVotedAt()
      );
      voteResults.add(movieResultDTO);
    }

    // 정렬
    voteResults.sort(
        // 득표수 오름차순 정렬 (comparingInt는 기본적으로 오름차순 정렬만 제공)
        Comparator.comparingInt(VoteMovieResultDTO::getVoteCount)
        // 내림차순 정렬로 변환
        .reversed()
        // 득표수가 같은 경우, 최신 득표 시간 기준 정렬
        .thenComparing(VoteMovieResultDTO::getLastVotedTime, Comparator.nullsLast(Comparator.reverseOrder()))
    );

    // 랭크 할당
    int rank = 1;
    for (VoteMovieResultDTO resultDTO : voteResults) {
      resultDTO.setRank(rank++);
    }

    return new VoteResultDTO(
        vote.getId(),
        vote.getStartDate(),
        vote.getEndDate(),
        vote.getEndDate().isAfter(LocalDateTime.now()),
        voteResults);
  }


  /**
   * 지난 주 1위 영화 가져오는 메서드
   */
  @Transactional(readOnly = true)
  public Long getLastWeekTopVoteMovie() {
    log.info("지난주 투표 1위 영화조회 시작");
    LocalDateTime now = LocalDateTime.now();
//    LocalDateTime lastWeekStart = now.minusWeeks(1).with(DayOfWeek.MONDAY).with((LocalTime.MIN));
//    LocalDateTime lastWeekEnd = now.minusWeeks(1).with(DayOfWeek.SATURDAY).with((LocalTime.MAX));
//    Vote lastWeekVote = voteRepository.findByStartDateBetween(lastWeekStart, lastWeekEnd)
//    Vote lastWeekVote = voteRepository.findByStartDateBetween(lastWeekStart, lastWeekEnd)
//        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_RESULT_NOT_FOUND));

    //테스트용
    //LocalDateTime lastStart = now.minusMinutes(10);
    LocalDateTime lastStart = now.minusHours(1);
    Vote lastWeekVote = voteRepository.findByStartDateBetween(lastStart, now)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    VoteResultDTO voteResults = getVoteResult(lastWeekVote.getId());

    if (voteResults.getResults().isEmpty()) {
      throw new DeepdiviewException(ErrorCode.VOTE_RESULT_NOT_FOUND);
    }

    VoteMovieResultDTO topVoteMovie = voteResults.getResults().get(0);
    Long tmdbId = topVoteMovie.getTmdbId();
    log.info("지난 주 투표 1위 영화 : tmdbId = {}, 득표수 = {}", tmdbId, topVoteMovie.getVoteCount());

    return tmdbId;
  }
}

