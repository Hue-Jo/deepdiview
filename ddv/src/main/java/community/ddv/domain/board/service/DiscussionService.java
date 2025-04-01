package community.ddv.domain.board.service;

import community.ddv.global.exception.ErrorCode;
import community.ddv.domain.board.dto.ReviewDTO;
import community.ddv.domain.board.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.movie.entity.Movie;
import community.ddv.domain.certification.CertificationService;
import community.ddv.domain.vote.service.VoteService;
import community.ddv.domain.user.entity.User;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.domain.movie.repostitory.MovieRepository;
import community.ddv.domain.user.service.UserService;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

  /**
   * 인증 승인 받은 사용자의 투표 1위 영화에 대한 리뷰 작성
   * @param reviewDTO
   */
  @Transactional
  public ReviewResponseDTO createDiscussion(ReviewDTO reviewDTO) {

    // 1. 인증 상태 확인
    User user = userService.getLoginUser();
    if (!certificationService.isUserCertified(user.getId())) {
      log.warn("인증되지 않은 사용자 : userId = {}", user.getId());
      throw new DeepdiviewException(ErrorCode.NOT_CERTIFIED_YET);
    }
    log.info("인증 상태 확인 완료 : userId = {}", user.getId());

    // 2. 지난주 1위를 한 영화인지 확인
    Long lastWeekTopMovieTmdbId = voteService.getLastWeekTopVoteMovie();
    Movie lastWeekTopMovie = movieRepository.findByTmdbId(lastWeekTopMovieTmdbId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.MOVIE_NOT_FOUND));
    log.info("지난 주 투표 1위 영화 확인 - TMDBId = {}, title = {}", lastWeekTopMovieTmdbId, lastWeekTopMovie.getTitle());

    // 3. 리뷰 작성 가능 기간 확인 (월-토)
    LocalDateTime now = LocalDateTime.now();
    if (now.getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("일요일은 리뷰 작성 불가");
      throw new DeepdiviewException(ErrorCode.INVALID_REVIEW_PERIOD);
    }
    log.info("리뷰 작성 가능 기간 확인 완료");

    // 4. 중복 리뷰 확인 -> 기존에 작성한 리뷰가 있으면 수정
    Review existingReview = reviewRepository.findByUserAndMovie(user, lastWeekTopMovie)
        .orElse(null);

    if (existingReview != null) {
      log.info("기존에 작성한 리뷰가 있으므로 수정 시도 : reviewId = {}", existingReview.getId());
      ReviewUpdateDTO updateDTO = new ReviewUpdateDTO(reviewDTO.getTitle(), reviewDTO.getContent(), reviewDTO.getRating());
      ReviewResponseDTO updatedReview = reviewService.updateReview(existingReview.getId(), updateDTO);
      existingReview.updateCertified(true);
      reviewRepository.save(existingReview);
      log.info("기존리뷰 수정완료 : reviewId = {}", existingReview.getId());
      return updatedReview;
    }

    // 5. 리뷰 생성 및 저장
    reviewDTO.setTmdbId(lastWeekTopMovie.getTmdbId());
    reviewDTO.setCertified(true);
    return reviewService.createReview(reviewDTO);
  }


  // 일요일 확인 API
  public boolean isTodaySunday() {
    return LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY;
  }
}
