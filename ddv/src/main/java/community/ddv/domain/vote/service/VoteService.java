package community.ddv.domain.vote.service;

import community.ddv.domain.movie.service.MovieService;
import community.ddv.global.exception.ErrorCode;
import community.ddv.domain.user.constant.Role;
import community.ddv.domain.movie.dto.MovieDTO;
import community.ddv.domain.vote.dto.VoteDTO.VoteCreatedDTO;
import community.ddv.domain.vote.dto.VoteDTO.VoteResultDTO;
import community.ddv.domain.vote.dto.VoteMovieResultDTO;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteOptionsDto;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteParticipationRequestDto;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteParticipationResponseDto;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.vote.entity.Vote;
import community.ddv.domain.vote.entity.VoteMovie;
import community.ddv.domain.vote.entity.VoteParticipation;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.vote.repository.VoteParticipationRepository;
import community.ddv.domain.vote.repository.VoteRepository;
import community.ddv.domain.user.service.UserService;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    //if (now.getDayOfWeek() != DayOfWeek.SUNDAY) {
    if (now.getDayOfWeek() != DayOfWeek.MONDAY) {
      log.error("투표 생성은 일요일만 가능합니다 : 현재요일 = {}", now.getDayOfWeek());
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_CREAT_DATE);
    }


    // 다음주에 진행할 투표가 이미 생성되어 있는지 확인
//    LocalDateTime nextWeekMondayStart = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(LocalTime.MIN);
//    LocalDateTime nextWeekSaturdayEnd = now.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).with(LocalTime.MAX);
//    boolean voteAlreadyExists = voteRepository.existsByStartDateBetween(nextWeekMondayStart, nextWeekSaturdayEnd);

    // 테스트용 임시
    LocalDateTime thisWeekStart = now.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
    LocalDateTime thisWeekEnd = now.with(DayOfWeek.SATURDAY).with(LocalTime.MAX);
    boolean voteAlreadyExists = voteRepository.existsByStartDateBetween(thisWeekStart, thisWeekEnd);



    if (voteAlreadyExists) {
      log.error("이미 생성한 투표가 있습니다.");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_VOTE);
    }

//    // 투표 시작일 : 생성 다음날(월요일) 자정(0시 0분)
//    LocalDateTime startDate = nextWeekMondayStart;
//    // 투표 종료일 : 토요일 23시 59분 59초
//    LocalDateTime endDate = nextWeekSaturdayEnd;

    // 테스트용 임시
    LocalDateTime startDate = thisWeekStart;
    LocalDateTime endDate = thisWeekEnd;

    // 인기도 탑 5의 영화 세부 정보 가져오기
    List<MovieDTO> top5Movies = movieService.getTop5Movies();
    log.info("인기도 탑5의 영화를 가져왔습니다.");

    Vote vote = Vote.builder()
        .title("다음주의 영화를 선택해주세요")
        .startDate(startDate)
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
   * @param voteParticipationRequestDto
   */
  @Transactional
  public VoteParticipationResponseDto participateVote(VoteParticipationRequestDto voteParticipationRequestDto) {

    User user = userService.getLoginUser();
    log.info("투표 시도 : userId = {} ", user.getId());

    LocalDateTime now = LocalDateTime.now();
    // 현재시간 전에 투표가 시작됐어야 하고, 현재시간 후로도 투표가 진행되고 있는, 즉 끝나지 않은 투표 조회
    Vote vote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD));

    // 중복참여 불가
    boolean alreadyParticipated = voteParticipationRepository.existsByUserAndVote(user, vote);
    if (alreadyParticipated) {
      log.error("이미 참여한 사용자");
      throw new DeepdiviewException(ErrorCode.AlREADY_VOTED);
    }

    // 영화 선택
    VoteMovie selectedVotedMovie = vote.getVoteMovies().stream()
        .filter(voteMovie -> voteMovie.getMovie().getTmdbId().equals(voteParticipationRequestDto.getTmdbId()))
        .findFirst()
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND_IN_VOTE));

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
   * 투표 결과 계산 메서드
   */
  private List<VoteMovieResultDTO> calculateVoteResult(Vote vote) {

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
    return voteResults;
  }

  /**
   * 투표 결과 추출 메서드
   */
  private VoteResultDTO createVoteResultDto(Vote vote) {
    List<VoteMovieResultDTO> voteResults = calculateVoteResult(vote);
    return new VoteResultDTO(
        vote.getId(),
        vote.getStartDate(),
        vote.getEndDate(),
        vote.getEndDate().isAfter(LocalDateTime.now()),
        voteResults
    );
  }

  /**
   * 특정투표 결과 조회
   * @param voteId
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getVoteResult(Long voteId){
    log.info("특정투표 결과 조회 시도 : voteId = {}", voteId);

    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    return createVoteResultDto(vote);
  }

  /**
   * 현재 진행중인 투표 결과 조회
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getCurrentVoteResult(){

    LocalDateTime now = LocalDateTime.now();
    Vote currentVote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    log.info("현재 진행중인 투표 결과 조회");
    return createVoteResultDto(currentVote);
  }


  /**
   * 가장 최근 진행됐던 투표 결과 조회
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getLatestVoteResult() {

    List<Vote> latestVote = voteRepository.findTop2ByOrderByStartDateDesc();

    Vote previousVote = latestVote.get(1); // 두 번쨰로 최신인 투표
    log.info("지난 투표 전체 결과 조회");
    return getVoteResult(previousVote.getId());
  }

  /**
   * 지난 주 1위 영화 가져오는 메서드
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "topRankMovie", key = "'topRankMovie'")
  public Long getLastWeekTopVoteMovie() {
    log.info("지난주 투표 1위 영화조회 시작");

    LocalDateTime now = LocalDateTime.now();
    // 본래 코드
    LocalDateTime lastWeekStart = now.minusWeeks(1).with(DayOfWeek.MONDAY).with(LocalTime.MIN);
    LocalDateTime lastWeekEnd = now.minusWeeks(1).with(DayOfWeek.SATURDAY).with(LocalTime.MAX);
    Vote lastWeekVote = voteRepository.findByStartDateBetween(lastWeekStart, lastWeekEnd)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));
    VoteResultDTO voteResults = getVoteResult(lastWeekVote.getId());

    // 테스트용코드
//    LocalDateTime lastStart = now.minusHours(1);
//    Vote lastWeekVote = voteRepository.findByStartDateBetween(lastStart, now)
//        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));
//
//    VoteResultDTO voteResults = getVoteResult(lastWeekVote.getId());

    if (voteResults.getResults().isEmpty()) {
      throw new DeepdiviewException(ErrorCode.VOTE_RESULT_NOT_FOUND);
    }

    VoteMovieResultDTO topVoteMovie = voteResults.getResults().get(0);
    Long tmdbId = topVoteMovie.getTmdbId();
    log.info("지난 투표 1위 영화 : tmdbId = {}, 득표수 = {}", tmdbId, topVoteMovie.getVoteCount());

    return tmdbId;
  }

  /**
   * 관리자의 투표 삭제 기능
   * @param voteId
   */
  @Transactional
  public void deleteVote(Long voteId) {

    User user = userService.getLoginUser();

    if (!user.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 투표 삭제 가능");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    log.info("투표 삭제 시도");
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    voteRepository.delete(vote);
    log.info("투표 삭제 완료 voteId = {}", voteId);

  }

  /**
   * 현재 진행중인 투표에 참여했는지 여부
   */
  @Transactional(readOnly = true)
  public boolean isUserAlreadyParticipatedInCurrentVote() {

    User user = userService.getLoginUser();
    LocalDateTime now = LocalDateTime.now();

    Vote currentVote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD));

    return voteParticipationRepository.existsByUserAndVote(user, currentVote);
  }
}

