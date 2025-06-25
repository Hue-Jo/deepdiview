package community.ddv.domain.user.service;

import community.ddv.domain.board.dto.ReviewRatingDTO;
import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.CommentRepository;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.certification.Certification;
import community.ddv.domain.certification.CertificationRepository;
import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import community.ddv.domain.user.dto.UserInfoDto.NicknameUpdateRequestDto;
import community.ddv.domain.user.dto.UserInfoDto.NicknameUpdateResponseDto;
import community.ddv.domain.user.dto.UserInfoDto.OneLineIntroRequestDto;
import community.ddv.domain.user.dto.UserInfoDto.OneLineIntroResponseDto;
import community.ddv.domain.user.dto.UserInfoDto.PasswordUpdateDto;
import community.ddv.domain.user.dto.UserInfoDto.UserInfoResponseDto;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.repository.UserRepository;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ReviewRepository reviewRepository;
  private final CommentRepository commentRepository;
  private final CertificationRepository certificationRepository;

  /**
   * 닉네임 수정
   */
  @Transactional
  public NicknameUpdateResponseDto updateNickname(NicknameUpdateRequestDto nicknameUpdateDto) {

    User user = getLoginUser();
    log.info("[UPDATE_NICKNAME] 닉네임 수정 시도: userId = {}, currentNickname = {}", user.getId(), user.getNickname());

    String newNickname = nicknameUpdateDto.getNewNickname();

    if (newNickname.equals(user.getNickname())) {
      return new NicknameUpdateResponseDto(user.getNickname());
    }

    //if (userRepository.findByNickname(newNickname).isPresent()) {
    if (userRepository.existsByNickname(newNickname)) {
      log.warn("[UPDATE_NICKNAME] 중복된 닉네임으로 수정 시도");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_NICKNAME);
    }

    user.updateNickname(newNickname);
    log.info("[UPDATE_NICKNAME] 닉네임 수정 완료: userId = {}, newNickname = {}", user.getId(), newNickname);
    return new NicknameUpdateResponseDto(newNickname);
  }

  @Transactional
  public void updatePassword(PasswordUpdateDto passwordUpdateDto) {

    User user = getLoginUser();
    log.info("[UPDATE_PASSWORD] 비밀번호 변경 시도: userId = {}", user.getId());

    String currentPassword = passwordUpdateDto.getCurrentPassword();
    String newPassword = passwordUpdateDto.getNewPassword();
    String newConfirmPassword = passwordUpdateDto.getNewConfirmPassword();

    // 사용중인 비밀번호 입력여부 확인
    if (currentPassword == null || currentPassword.isBlank()) {
      log.warn("[UPDATE_PASSWORD] 현재 사용중인 비밀번호 불일치");
      throw new DeepdiviewException(ErrorCode.EMPTY_PASSWORD);
    }
    // 사용중인 비밀번호 일치여부 확인
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    // 비밀번호 변경 시도 여부 확인
    if ((newPassword != null && !newPassword.isBlank()) || (newConfirmPassword != null && !newConfirmPassword.isBlank())) {
      log.info("[UPDATE_PASSWORD] 새 비밀번호 변경 시도");

      // 값이 제대로 입력됐는지 (둘 중 하나라도 비어있으면 예외) 확인
      if (newPassword == null || newConfirmPassword == null || newPassword.isBlank() || newConfirmPassword.isBlank()) {
        log.warn("[UPDATE_PASSWORD] 새 비밀번호 / 확인 값이 입력되지 않음");
        throw new DeepdiviewException(ErrorCode.EMPTY_PASSWORD);
      }
      if (!newPassword.equals(newConfirmPassword)) {
        log.warn("[UPDATE_PASSWORD] 새 비밀번호와 확인 값이 일치하지 않음");
        throw new DeepdiviewException(ErrorCode.NOT_MATCHED_PASSWORD);
      }
      user.updatePassword(passwordEncoder.encode(newPassword));
    }
    log.info("[UPDATE_PASSWORD] 비밀번호 변경 완료");
  }


  /**
   * 한줄소개 설정/수정/삭제
   * @param oneLineIntro
   */
  @Transactional
  public OneLineIntroResponseDto updateOneLineIntro(OneLineIntroRequestDto oneLineIntro) {

    User user = getLoginUser();
    log.info("[ONE_LINE_INTRO] 한줄소개 수정 시도 : userId = {}", user.getId());

    String newOneLineIntro = oneLineIntro.getOneLineIntro();
    String newONeLineIntroResponse = (newOneLineIntro == null || newOneLineIntro.isEmpty()) ? null : newOneLineIntro;

    // 기존의 한줄소개가 없었던 경우
    if (user.getOneLineIntroduction() == null) {
      if (newONeLineIntroResponse != null) {
        user.updateOneLineIntroduction(newONeLineIntroResponse);
        log.info("[ONE_LINE_INTRO] 한줄소개 설정 완료");
      }
    } else {
      // 기존의 한줄 소개가 존재하는 경우
      if (newONeLineIntroResponse == null) {
        user.updateOneLineIntroduction(null);
        log.info("[ONE_LINE_INTRO] 한줄소개 삭제 완료");
      } else {
        user.updateOneLineIntroduction(newONeLineIntroResponse);
        log.info("[ONE_LINE_INTRO] 한줄소개 수정 완료");
      }
    }
    userRepository.save(user);
    return new OneLineIntroResponseDto(newONeLineIntroResponse);
  }


  /**
   * 내 정보 조회
   */
  @Transactional(readOnly = true)
  public UserInfoResponseDto getMyInfo() {

    User user = getLoginUser();
    log.info("[MY_INFO] 내 정보 조회: userId = {}", user.getId());

    int reviewCount = reviewRepository.countByUser_Id(user.getId());
    int commentCount = commentRepository.countByUser_Id(user.getId());

    List<Review> reviews = reviewRepository.findAllByUser_Id(user.getId());
    ReviewRatingDTO ratingStats = getRatingStats(reviews);

    LocalDate today = LocalDate.now();
    // 일요일이 되면 다음주 토요일로 계산 되는 것 방지용 (일요일을 토요일처럼 생각)
    if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
      today = today.minusDays(1);
    }
    // 이번주 내에 인증한 게 있는지 조회
    LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    Certification certification = certificationRepository
        // 이번 주(월~토)에 한 인증 중에서 가장 최신 인증 1개 가져오기
        .findTopByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            user.getId(),
            startOfWeek.atStartOfDay(),
            endOfWeek.atTime(LocalTime.MAX)
        ).orElse(null);

    CertificationStatus certificationStatus =
        certification != null && certification.getStatus() != null
            ? certification.getStatus() : CertificationStatus.NONE;

    RejectionReason rejectionReason =
        certification != null && certification.getStatus() == CertificationStatus.REJECTED
            ? certification.getRejectionReason() : null;

    log.info("[MY_INFO] 내 정보 조회 완료");
    return UserInfoResponseDto.builder()
        .nickname(user.getNickname())
        .email((user.getEmail()))
        .profileImageUrl(user.getProfileImageUrl())
        .oneLineIntro(user.getOneLineIntroduction())
        .reviewCount(reviewCount)
        .commentCount(commentCount)
        .ratingStats(ratingStats)
        .certificationStatus(certificationStatus)
        .rejectionReason(rejectionReason)
        .build();
  }

  /**
   * 특정 사용자 정보 확인
   * @param userId
   */
  @Transactional(readOnly = true)
  public UserInfoResponseDto getOthersInfo(Long userId) {

    getLoginUser();
    log.info("[GET_OTHERS_INFO] 타인 정보 조회 시도: userId = {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("[GET_OTHERS_INFO] 사용자 정보 조회 실패: userId = {}", userId);
          return new DeepdiviewException(ErrorCode.USER_NOT_FOUND);
        });

    int reviewCount = reviewRepository.countByUser_Id(userId);
    int commentCount = commentRepository.countByUser_Id(userId);

    List<Review> reviews = reviewRepository.findAllByUser_Id(user.getId());
    ReviewRatingDTO ratingStats = getRatingStats(reviews);

    log.info("[GET_OTHERS_INFO] 타인 정보 조회 완료: userId = {}", userId);

    return UserInfoResponseDto.builder()
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .oneLineIntro(user.getOneLineIntroduction())
        .reviewCount(reviewCount)
        .commentCount(commentCount)
        .ratingStats(ratingStats)
        .build();
  }

  private ReviewRatingDTO getRatingStats(List<Review> reviews) {

    Map<Double, Long> distribution = reviews.stream()
        .map(Review::getRating)
        .collect(Collectors.groupingBy(
            rating -> rating,
            Collectors.counting()
        ));

    Map<Double, Integer> ratingDistribution = new LinkedHashMap<>();
    for (double i = 0.5; i <= 5.0; i += 0.5) {
      ratingDistribution.put(i, distribution.getOrDefault(i, 0L).intValue());
    }

    double ratingAverage = reviews.stream()
        .mapToDouble(Review::getRating)
        .average()
        .orElse(0.0);

    double roundedRating = Math.round(ratingAverage*10.0)/10.0;

    return ReviewRatingDTO.builder()
        .ratingAverage(roundedRating)
        .ratingDistribution(ratingDistribution)
        .build();
  }

  // 로그인 여부 확인 메서드
  public User getLoginUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new DeepdiviewException(ErrorCode.UNAUTHORIZED);
    }

    String email = authentication.getName();

    return userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));
  }

  public User getLoginOrNull() {
    try {
      return getLoginUser();
    } catch (DeepdiviewException e) {
      return null;
    }
  }
}

