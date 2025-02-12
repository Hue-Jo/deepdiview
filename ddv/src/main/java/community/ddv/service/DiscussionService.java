package community.ddv.service;

import community.ddv.constant.ErrorCode;
import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.entity.Movie;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.MovieRepository;
import community.ddv.repository.ReviewRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionService {

  private final ReviewService reviewService;
  private final VoteService voteService;
  private final CertificationService certificationService;
  private final UserService userService;

  private final MovieRepository movieRepository;
  private final ReviewRepository reviewRepository;

   //인증 승인 받은 사용자의 투표 1위 영화에 대한 리뷰 작성
  @Transactional
  public ReviewResponseDTO createDiscussion(ReviewDTO reviewDTO) {

    // 1. 인증 상태 확인
    User user = userService.getLoginUser();
    if (!certificationService.isUserCertified(user.getId())) {
      log.warn("인증되지 않은 사용자");
      throw new DeepdiviewException(ErrorCode.NOT_CERTIFIED_YET);
    }
    log.info("인증 상태 확인 완료");

    // 2. 지난주 1위를 한 영화인지 확인
    Long lastWeekTopMovieTmdbId = voteService.getLastWeekTopVoteMovie();
    Movie lastWeekTopMovie = movieRepository.findByTmdbId(lastWeekTopMovieTmdbId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));
    log.info("지난 주 1위 영화 확인");

    // 3. 리뷰 작성 가능 기간 확인 (월-토)
    LocalDateTime now = LocalDateTime.now();
    if (now.getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("일요일은 리뷰 작성 불가");
      throw new DeepdiviewException(ErrorCode.INVALID_REVIEW_PERIOD);
    }

    // 4. 중복 리뷰 확인
    if (reviewRepository.existsByUserAndMovie(user, lastWeekTopMovie)) {
      log.warn("이미 리뷰가 작성된 영화");
      throw new DeepdiviewException(ErrorCode.ALREADY_COMMITED_REVIEW);
    }

    // 5. 리뷰 생성 및 저장
    reviewDTO.setTmdbId(lastWeekTopMovie.getTmdbId());
    return reviewService.createReview(reviewDTO);
  }
}
