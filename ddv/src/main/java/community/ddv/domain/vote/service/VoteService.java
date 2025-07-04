package community.ddv.domain.vote.service;

import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.user.constant.Role;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.domain.vote.dto.VoteResultDTO;
import community.ddv.domain.vote.dto.VoteMovieResultDTO;
import community.ddv.domain.vote.dto.VoteOptionsDTO;
import community.ddv.domain.vote.dto.VoteParticipationDTO;
import community.ddv.domain.vote.entity.Vote;
import community.ddv.domain.vote.entity.VoteMovie;
import community.ddv.domain.vote.entity.VoteParticipation;
import community.ddv.domain.vote.repository.VoteParticipationRepository;
import community.ddv.domain.vote.repository.VoteRepository;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

  /**
   * 관리자의 투표 생성 한 주에는 한 번만 투표 생성 가능
   * 투표 생성은 일요일만 가능하지만, 실제 투표 시작은 월요일 0시 0분부터
   * 투표 종료일 : 해당 주 토요일 23시 59분 59초
   */
  @Transactional
  public void createVote() {

    // 로그인된 관리자만 투표 생성 가능
    User admin = userService.getLoginUser();
    log.info("[CREATE_VOTE] 투표 생성 시도: userId = {}", admin.getId());
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 투표를 생성할 수 있습니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    // 투표 생성은 일요일만 가능, 한 주에 한 번만 가능
    LocalDateTime now = LocalDateTime.now();
    if (now.getDayOfWeek() != DayOfWeek.SUNDAY) {
      log.error("[CREATE_VOTE] 투표 생성은 일요일만 가능 : 현재요일 = {}", now.getDayOfWeek());
      throw new DeepdiviewException(ErrorCode.INVALID_VOTE_CREATE_DATE);
    }

    // 다음주에 진행할 투표가 이미 생성되어 있는지 확인
    LocalDateTime nextWeekMondayStart = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(LocalTime.MIN);
    LocalDateTime nextWeekSaturdayEnd = now.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).with(LocalTime.MAX);
    boolean voteAlreadyExists = voteRepository.existsByStartDateBetween(nextWeekMondayStart, nextWeekSaturdayEnd);

    // 테스트용 임시
//    LocalDateTime thisWeekStart = now.with(DayOfWeek.MONDAY).with(LocalTime.MIN);
//    LocalDateTime thisWeekEnd = now.with(DayOfWeek.SATURDAY).with(LocalTime.MAX);
//    boolean voteAlreadyExists = voteRepository.existsByStartDateBetween(thisWeekStart, thisWeekEnd);

    if (voteAlreadyExists) {
      log.error("[CREATE_VOTE] 이미 다음 주에 생성된 투표가 존재함");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_VOTE);
    }

    // 투표 시작일 : 생성 다음날(월요일) 자정(0시 0분)
    LocalDateTime startDate = nextWeekMondayStart;
    // 투표 종료일 : 토요일 23시 59분 59초
    LocalDateTime endDate = nextWeekSaturdayEnd;

    // 테스트용 임시
//    LocalDateTime startDate = thisWeekStart;
//    LocalDateTime endDate = thisWeekEnd;

    // 과거에 1위를 했던 영화들 가져오기
    Set<Long> pastTopMovies = getAllPastTopRankMovies();
    // 과거 1위 했던 영화 빼고 인기도 탑 6개 가져오기
    Pageable topMoviesSize = PageRequest.of(0, 6);
    List<Movie> top6Movies = movieRepository.findTop6RankExcludedTmdbIds(pastTopMovies, topMoviesSize);

    Vote vote = Vote.builder()
        .title("다음주의 영화를 선택해주세요")
        .startDate(startDate)
        .endDate(endDate)
        .voteMovies(new ArrayList<>())
        .build();

    // 선택된 영화들을 VoteMovie 테이블에 저장
    for (Movie movie : top6Movies) {
      VoteMovie voteMovie = VoteMovie.builder()
          .vote(vote)
          .movie(movie)
          .build();
      vote.getVoteMovies().add(voteMovie);
    }
    Vote savedVote = voteRepository.save(vote);
    log.info("[CREATE_VOTE] 투표 생성 완료 : voteId = {}, 시작시간 = {}, 종료시간 = {}", savedVote.getId(), savedVote.getStartDate(), savedVote.getEndDate());

//    List<Long> tmdbIds = savedVote.getVoteMovies().stream()
//        .map(voteMovie -> voteMovie.getMovie().getTmdbId())
//        .toList();
//    return new VoteOptionsDto(tmdbIds);

  }

  // 과거 투표 1위했던 영화 조회 메서드
  public Set<Long> getAllPastTopRankMovies() {
    List<Vote> allVotes = voteRepository.findAll();
    Set<Long> topTmdbIds = new HashSet<>();
    for (Vote vote : allVotes) {
      List<VoteMovieResultDTO> resultDTOS = calculateVoteResult(vote);
      if (!resultDTOS.isEmpty()) {
        topTmdbIds.add(resultDTOS.get(0).getTmdbId());
      }
    }
    return topTmdbIds;
  }


  /**
   * 현재 진행중인 투표의 선택지 조회 tmdbIds 반환
   */
  @Transactional(readOnly = true)
  public VoteOptionsDTO getVoteChoices() {

    userService.getLoginUser();
    log.info("[VOTE] 투표 선택지 조회 시도");

    // 현재 진행중인 투표 조회
    LocalDateTime now = LocalDateTime.now();
    Optional<Vote> activatingVote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now);

    // 투표가 진행중이지는 않지만 관리자가 투표를 생성해둔 경우 (일요일) 다음주의 투표 선택지 보여주기
    if (activatingVote.isEmpty() && now.getDayOfWeek() == DayOfWeek.SUNDAY) {
      activatingVote = voteRepository.findFirstByStartDateAfterOrderByStartDateAsc(now);
    }
    // 일요일에 투표가 생성되지 않은 경우에는 빈 리스트 반환
    if (activatingVote.isEmpty()) {
      return new VoteOptionsDTO(Collections.emptyList());
    }

    Vote vote = activatingVote.get();

    // 선택지의 tmdbId 추출
    List<Long> tmdbIds = vote.getVoteMovies().stream()
        .map(voteMovie -> voteMovie.getMovie().getTmdbId())
        .collect(Collectors.toList());

    log.info("[VOTE] 투표 선택지 조회 완료");
    return new VoteOptionsDTO(tmdbIds);
  }

  /**
   * 일반 사용자의 투표 참여
   * 중복 참여 불가
   * @param voteParticipationRequestDto
   */
  @Transactional
  public void participateVote(VoteParticipationDTO voteParticipationRequestDto) {

    User user = userService.getLoginUser();
    log.info("[VOTE] 투표 시도: userId = {}", user.getId());

    LocalDateTime now = LocalDateTime.now();
    // 현재시간 전에 투표가 시작됐어야 하고, 현재시간 후로도 투표가 진행되고 있는, 즉 끝나지 않은 투표 조회
    Vote vote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now)
        .orElseThrow(() -> {
          log.error("[VOTE] 투표 기간이 아님");
          return new DeepdiviewException(ErrorCode.INVALID_VOTE_PERIOD);
        });

    // 중복참여 불가
    boolean alreadyParticipated = voteParticipationRepository.existsByUserAndVote(user, vote);
    if (alreadyParticipated) {
      log.warn("[VOTE] 이미 투표에 참여한 사용자: userId = {}", user.getId());
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

    log.info("[VOTE] 투표 참여 완료: userId = {}, voteId = {}, tmdbId = {}", user.getId(), vote.getId(), selectedVotedMovie.getMovie().getTmdbId());

  }

  /**
   * 투표 결과 계산 메서드
   */
  private List<VoteMovieResultDTO> calculateVoteResult(Vote vote, Long userId) {

    // 1. 사용자가 선택한 영화 Id 찾기
    Long selectedTmdbId = null;

    if (userId != null) {
      selectedTmdbId = vote.getVoteParticipations().stream()
          .filter(voteParticipation -> voteParticipation.getUser().getId().equals(userId))
          .map(voteParticipation -> voteParticipation.getSelectedVoteMovie().getMovie().getTmdbId())
          .findFirst() // 한 투표에서는 한 번만 참여 가능이니까
          .orElse(null);
    }

    // 2. 득표수 기준 내림차순 정렬 -> 동수 발생시, 마지막 투표시간 기준 내림차순 정렬
    List<VoteMovie> sortedVoteMovies = vote.getVoteMovies().stream()
        .sorted(
            Comparator.comparing(VoteMovie::getVoteCount).reversed()
                .thenComparing(
                    VoteMovie::getLastVotedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())) // null은 마지막으로
        ).toList();

    // 3. 정렬된 투표결과 DTO 생성
    List<VoteMovieResultDTO> voteResults = new ArrayList<>();
    int rank = 1;
    for (VoteMovie voteMovie : sortedVoteMovies) {
      Movie movie = voteMovie.getMovie();
      VoteMovieResultDTO voteResultDto = VoteMovieResultDTO.builder()
          .tmdbId(movie.getTmdbId())
          .voteCount(voteMovie.getVoteCount())
          .rank(rank++)
          .lastVotedTime(voteMovie.getLastVotedAt())
          .voted(userId != null ? voteMovie.getMovie().getTmdbId().equals(selectedTmdbId) : null)
          .build();
      voteResults.add(voteResultDto);
    }
    return voteResults;
  }

  private List<VoteMovieResultDTO> calculateVoteResult(Vote vote) {
    return calculateVoteResult(vote, null);
  }

  /**
   * 투표 결과 추출 메서드
   */
  private VoteResultDTO createVoteResultDto(Vote vote, Long userId) {
    List<VoteMovieResultDTO> voteResults = calculateVoteResult(vote, userId);;
    return new VoteResultDTO(voteResults);
  }

  /**
   * 특정투표 결과 조회
   * @param voteId
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getVoteResult(Long voteId, Long userId){
    log.info("[VOTE] 특정투표 결과 조회 시도 : voteId = {}", voteId);

    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    return createVoteResultDto(vote, userId);
  }

  /**
   * 현재 진행중인 투표 결과 조회
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getCurrentVoteResult(Long userId){

    LocalDateTime now = LocalDateTime.now();
    Vote currentVote = voteRepository.findByStartDateBeforeAndEndDateAfter(now, now)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    log.info("[VOTE] 현재 진행중인 투표 결과 조회");
    return createVoteResultDto(currentVote, userId);
  }


  /**
   * 가장 최근 진행됐던 투표 결과 조회
   */
  @Transactional(readOnly = true)
  public VoteResultDTO getLatestVoteResult() {

    List<Vote> latestVote = voteRepository.findTop2ByOrderByStartDateDesc();

    Vote previousVote = latestVote.get(1); // 두 번쨰로 최신인 투표
    log.info("[VOTE] 지난 투표 전체 결과 조회");
    return getVoteResult(previousVote.getId(), null);
  }

  /**
   * 지난 주 1위 영화 가져오는 메서드
   */
  @Transactional(readOnly = true)
  @Cacheable(value = "topRankMovie", key = "'topRankMovie'")
  public Long getLastWeekTopVoteMovie() {
    log.info("[VOTE] 지난주 투표 1위 영화조회 시작");

    LocalDateTime now = LocalDateTime.now();
    // 본래 코드
    // 지난주 일요일에 생성된 투표 찾기
    LocalDate lastSunday = now.toLocalDate().minusWeeks(1).with(DayOfWeek.SUNDAY);
    LocalDateTime voteStarted = lastSunday.atStartOfDay(); // 지난주 일요일 0시 0분 0초
    LocalDateTime voteEnded = lastSunday.atTime(LocalTime.MAX);  // 지난주 일요일 23시 59분 59초

    Vote lastWeekVote = voteRepository.findByStartDateBetween(voteStarted, voteEnded)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    VoteResultDTO voteResults = getVoteResult(lastWeekVote.getId(), null);

    if (voteResults.getResults().isEmpty()) {
      throw new DeepdiviewException(ErrorCode.VOTE_RESULT_NOT_FOUND);
    }

    VoteMovieResultDTO topVoteMovie = voteResults.getResults().get(0);
    Long tmdbId = topVoteMovie.getTmdbId();
    log.info("[VOTE] 지난 투표 1위 영화 : tmdbId = {}, 득표수 = {}", tmdbId, topVoteMovie.getVoteCount());

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
      log.error("[VOTE] 관리자만 투표 삭제 가능");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    log.info("[VOTE] 투표 삭제 시도");
    Vote vote = voteRepository.findById(voteId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.VOTE_NOT_FOUND));

    voteRepository.delete(vote);
    log.info("[VOTE] 투표 삭제 완료 voteId = {}", voteId);

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

