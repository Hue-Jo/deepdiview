package community.ddv.domain.user.service;

import community.ddv.domain.board.entity.Review;
import community.ddv.domain.board.repository.CommentRepository;
import community.ddv.domain.board.repository.ReviewRepository;
import community.ddv.domain.certification.Certification;
import community.ddv.domain.certification.CertificationRepository;
import community.ddv.domain.certification.constant.RejectionReason;
import community.ddv.domain.user.dto.LoginResponse;
import community.ddv.domain.user.dto.UserDTO.AccountDeleteDto;
import community.ddv.domain.user.dto.UserDTO.AccountUpdateDto;
import community.ddv.domain.user.dto.UserDTO.LoginDto;
import community.ddv.domain.user.dto.UserDTO.OneLineIntro;
import community.ddv.domain.user.dto.UserDTO.SignUpDto;
import community.ddv.domain.user.dto.UserDTO.TokenDto;
import community.ddv.domain.user.dto.UserDTO.UserInfoDto;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.repository.UserRepository;
import community.ddv.global.component.JwtProvider;
import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.user.constant.Role;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
  private final RedisTemplate<String, String> redisRefreshTokenTemplate;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final ReviewRepository reviewRepository;
  private final CommentRepository commentRepository;
  private final CertificationRepository certificationRepository;

  /**
   * 회원가입
   * @param signUpDto - 이메일(중복 불가), 비밀번호, 비밀번호 확인, 닉네임(중복 불가)
   */
  @Transactional
  public void signUp(SignUpDto signUpDto) {
    log.info("회원가입 시도");

    // 같은 이메일로 중복 회원가입 불가
    if (userRepository.findByEmail(signUpDto.getEmail()).isPresent()) {
      log.warn("중복 회원가입 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_MEMBER);
    }
    // 비밀번호 확인 로직 통과 여부
    if (!signUpDto.getPassword().equals(signUpDto.getConfirmPassword())) {
      log.warn("비밀번호 확인 로직 통과 X");
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }
    // 중복 닉네임 사용불가
    if (userRepository.findByNickname(signUpDto.getNickname()).isPresent()) {
      log.warn("중복 닉네임 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_NICKNAME);
    }

    User user = User.builder()
        .email(signUpDto.getEmail())
        .password(passwordEncoder.encode(signUpDto.getPassword()))
        .nickname(signUpDto.getNickname())
        .role(Role.USER) // 일반 유저를 기본 역할로 설정
        .build();

    userRepository.save(user);
    log.info("회원가입 완료");
  }

  /**
   * 로그인
   * @param loginDto - 이메일, 비밀번호
   */
  public LoginResponse logIn(LoginDto loginDto) {
    log.info("로그인 시도 : {} ", loginDto.getEmail());

    // 해당 이메일로 가입된 유저가 존재하는지 확인
    User user = userRepository.findByEmail(loginDto.getEmail())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    // 비밀번호 검증
    if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    // 엑세스 토큰 생성
    String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole());

    String refreshToken = redisRefreshTokenTemplate.opsForValue().get(user.getEmail());
    // 리프레시 토큰 생성 (없거나 만료되었으면 새로 생성)
    if (refreshToken == null || jwtProvider.isTokenExpired(refreshToken)) {
      refreshToken = generateAndStoreRefreshToken(user);
    } else {
      log.info("유효한 리프레시 토큰 사용");
    }

    log.info("로그인 성공");
    return new LoginResponse(
        accessToken,
        refreshToken,
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getProfileImageUrl(),
        user.getRole()
    );
  }

  private String generateAndStoreRefreshToken(User user) {
    String newRefreshToken = jwtProvider.generateRefreshToken(user.getEmail(), user.getRole());
    // set(키, 값, 숫자, TimeUnit)으로  일정시간(15일) 후 자동 삭제되는 토큰 저장
    redisRefreshTokenTemplate.opsForValue().set(user.getEmail(), newRefreshToken, 15, TimeUnit.DAYS);
    return newRefreshToken;
  }

  /**
   * 로그아웃
   */
  @Transactional
  public void logout() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    log.info("로그아웃 요청");

    // 현재 인증된 사용자 이메일과 요청에 포함된 이메일이 동일한지 확인
    userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    // 리프레시 토큰 삭제
    Boolean deletedRefreshToken = redisRefreshTokenTemplate.delete(email);
    // NullPointException 방지를 위해 Boolean 객체 사용
    if (Boolean.TRUE.equals(deletedRefreshToken)) {
      log.info("리프레시 토큰 삭제 완료");
    } else {
      log.warn("로그아웃 실패");
    }
    // SecurityContext 초기화
    SecurityContextHolder.clearContext();
    log.info("로그아웃 성공");
  }

  /**
   * 리프레시 토큰으로 엑세스 토큰 재발급
   * @param refreshToken
   */
  public TokenDto reissueAccessToken(String refreshToken) {
    log.info("리프레시 토큰으로 엑세스 토큰 재발급 요청");

    // 리프레시 토큰이 유효한지 확인
    if (jwtProvider.isTokenExpired(refreshToken)) {
      throw new DeepdiviewException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 리프레시 토큰에서 이메일 추출 후, Redis에 저장된 토큰과 일치 여부 확인
    String email = jwtProvider.extractEmail(refreshToken);
    String storedRefreshToken = redisRefreshTokenTemplate.opsForValue().get(email);
    if(!refreshToken.equals(storedRefreshToken)) {
      throw new DeepdiviewException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 사용자 정보 가져오기
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    // 새 엑세스 토큰 생성/발급
    String newAccessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole());

    log.info("엑세스 토큰 재발급 완료");
    return new TokenDto(newAccessToken);
  }

  /**
   * 회원탈퇴
   * @param accountDeleteDto - 로그인 된 상태에서 비밀번호 입력
   */
  @Transactional
  public void deleteAccount(AccountDeleteDto accountDeleteDto) {

    User user = getLoginUser();
    log.info("회원탈퇴 요청 : {}", user.getEmail());

    if (user.getRole() == Role.ADMIN) {
      throw new DeepdiviewException(ErrorCode.ADMIN_CANNOT_BE_DELETED);
    }

    // 비밀번호 확인
    if (!passwordEncoder.matches(accountDeleteDto.getPassword(), user.getPassword())) {
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    redisRefreshTokenTemplate.delete(user.getEmail());
    log.info("리프레시 토큰 삭제");

    // 사용자 삭제
    userRepository.delete(user);
    log.info("회원탈퇴 완료");

    SecurityContextHolder.clearContext();
    log.info("SecurityContext 초기화");
  }

  /**
   * 회원정보 수정 (닉네임, 비밀번호)
   * @param accountUpdateDto
   */
  @Transactional
  public void updateAccount(AccountUpdateDto accountUpdateDto) {

    User user = getLoginUser();
    log.info("회원정보 수정시도 : {}", user.getEmail());

    String newNickname = accountUpdateDto.getNewNickname();
    String newPassword = accountUpdateDto.getNewPassword();
    String newConfirmPassword = accountUpdateDto.getNewConfirmPassword();

    // 닉네임 변경 시도
    if (!newNickname.isBlank()) {
      log.info("닉네임 변경시도 {} -> {}", user.getNickname(), newNickname);

      // 이미 존재하는 닉네임으로는 변경 불가 (자신이 예전에 쓰던 닉네임 포함)
      if (userRepository.findByNickname(newNickname).isPresent()) {
        log.info("이미 존재하는 닉네임이 있어 해당 닉네임으로 변경 불가");
        throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_NICKNAME);
      }
      // 존재하지 않는 닉네임일 시, 닉네임 변경 성공
      user.updateNickname(newNickname);
    }

    if (!newPassword.isBlank()) {
      log.info("비밀번호 변경시도");
      if (!isValidPassword(newPassword)) {
        throw new DeepdiviewException(ErrorCode.NOT_ENOUGH_PASSWORD);
      } else {
        if (!newPassword.equals(newConfirmPassword)) {
          throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
        }
      }
      user.updatePassword(passwordEncoder.encode(newPassword));
    }

    log.info("회원정보 수정완료");

  }

  // 비밀번호 8자 이상 작성해야 유효
  private boolean isValidPassword(String password) {
    String regex = "\\w{8,}";
    return Pattern.compile(regex).matcher(password).matches();
  }

  /**
   * 한줄소개 설정/수정/삭제
   * @param oneLineIntro
   */
  @Transactional
  public void updateOneLineIntro(OneLineIntro oneLineIntro) {

    User user = getLoginUser();
    log.info("한줄소개 수정 시도 : {}", user.getEmail());

    String newOneLineIntro = oneLineIntro.getOneLineIntro();

    // 기존의 한줄소개가 없었던 경우
    if (user.getOneLineIntroduction() == null) {
      if (!newOneLineIntro.isEmpty()) {
        user.updateOneLineIntroduction(newOneLineIntro);
        log.info("한줄소개 설정 완료");
      }
    } else {
      // 기존의 한줄 소개가 존재하는 경우
      if (newOneLineIntro.isEmpty()) {
        user.updateOneLineIntroduction(null);
        log.info("한줄소개 삭제 완료");
      } else {
        user.updateOneLineIntroduction(newOneLineIntro);
        log.info("한줄소개 수정 완료");
      }
    }
    userRepository.save(user);
  }


  /**
   * 내 정보 조회
   */
  @Transactional(readOnly = true)
  public UserInfoDto getMyInfo() {

    User user = getLoginUser();

    int reviewCount = reviewRepository.countByUser_Id(user.getId());
    int commentCount = commentRepository.countByUser_Id(user.getId());

    Map<Double, Long> ratingDistribution
        = reviewRepository.findAllByUser_Id(user.getId()) // List<Review> 가져옴
        .stream() // Stream<Review>로 변환
        .map(Review::getRating) // Stream<Double> 로 변환
        .collect(Collectors.groupingBy(
            rating -> rating,
            Collectors.counting()
        ));

    Certification certification = certificationRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId()).orElse(null);
    CertificationStatus certificationStatus =
        certification != null ?
        certification.getStatus() : null;
    RejectionReason rejectionReason =
        certification != null && certification.getStatus() == CertificationStatus.REJECTED ?
        certification.getRejectionReason() : null;


    return UserInfoDto.builder()
        .nickname(user.getNickname())
        .email((user.getEmail()))
        .profileImageUrl(user.getProfileImageUrl())
        .oneLineIntro(user.getOneLineIntroduction())
        .reviewCount(reviewCount)
        .commentCount(commentCount)
        .ratingDistribution(ratingDistribution)
        .certificationStatus(certificationStatus)
        .rejectionReason(rejectionReason)
    .build();
  }

  /**
   * 특정 사용자 정보 확인
   * @param userId
   */
  @Transactional(readOnly = true)
  public UserInfoDto getOthersInfo(Long userId) {

    getLoginUser();
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    int reviewCount = reviewRepository.countByUser_Id(userId);
    int commentCount = commentRepository.countByUser_Id(userId);

    Map<Double, Long> ratingDistribution = reviewRepository.findAllByUser_Id(user.getId()).stream()
        .map(Review::getRating)
        .collect(Collectors.groupingBy(
            rating -> rating,
            Collectors.counting()
        ));

    return UserInfoDto.builder()
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .oneLineIntro(user.getOneLineIntroduction())
        .reviewCount(reviewCount)
        .commentCount(commentCount)
        .ratingDistribution(ratingDistribution)
        .build();
  }

  // 로그인 여부 확인 메서드
  @Transactional(readOnly = true)
  public User getLoginUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new DeepdiviewException(ErrorCode.UNAUTHORIZED);
    }

    String email = authentication.getName();

    return userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));
  }

  @Transactional(readOnly = true)
  public User getLoginOrNull() {
    try {
      return getLoginUser();
    } catch (DeepdiviewException e) {
      return null;
    }
  }
}

