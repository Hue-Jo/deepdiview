package community.ddv.domain.user.service;

import community.ddv.domain.notification.NotificationService;
import community.ddv.domain.user.constant.Role;
import community.ddv.domain.user.dto.SignDto.AccountDeleteDto;
import community.ddv.domain.user.dto.SignDto.LoginDto;
import community.ddv.domain.user.dto.SignDto.LoginResponseDto;
import community.ddv.domain.user.dto.SignDto.SignUpDto;
import community.ddv.domain.user.dto.SignDto.TokenDto;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.repository.UserRepository;
import community.ddv.global.component.JwtProvider;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final UserService userService;
  private final NotificationService notificationService;
  private final UserRepository userRepository;
  private final RedisTemplate<String, String> redisStringTemplate;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final EmailService emailService;

  @Value("${profile.image.default-url}")
  private String defaultProfileImageUrl;

  /**
   * 회원가입
   * @param signUpDto - 이메일(중복 불가), 비밀번호, 비밀번호 확인, 닉네임(중복 불가)
   */
  @Transactional
  public void signUp(SignUpDto signUpDto) {
    log.info("[SIGNUP] 회원가입 시도: email={}", signUpDto.getEmail());

    // 이메일 인증여부 확인
    if (!emailService.isVerified(signUpDto.getEmail())) {
      log.warn("[SIGNUP] 이메일 인증하지 않음: {}", signUpDto.getEmail());
    throw new DeepdiviewException(ErrorCode.EMAIL_NOT_VERIFIED);
    }
    // 같은 이메일로 중복 회원가입 불가
    if (userRepository.existsByEmail(signUpDto.getEmail())) {
      log.warn("[SIGNUP] 중복 이메일 회원가입 시도: {}", signUpDto.getEmail());
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_MEMBER);
    }
    // 비밀번호 확인 로직 통과 여부
    if (!signUpDto.getPassword().equals(signUpDto.getConfirmPassword())) {
      log.warn("[SIGNUP] 비밀번호 확인 로직 통과 X");
      throw new DeepdiviewException(ErrorCode.NOT_MATCHED_PASSWORD);
    }
    // 중복 닉네임 사용불가
    if (userRepository.existsByNickname(signUpDto.getNickname())) {
      log.warn("[SIGNUP] 중복 닉네임 사용불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_NICKNAME);
    }

    User user = User.builder()
        .email(signUpDto.getEmail())
        .password(passwordEncoder.encode(signUpDto.getPassword()))
        .nickname(signUpDto.getNickname())
        .profileImageUrl(defaultProfileImageUrl)
        .role(Role.USER) // 일반 유저를 기본 역할로 설정
        .build();

    userRepository.save(user);
    redisStringTemplate.delete("EMAIL_VERIFIED:" + signUpDto.getEmail());
    log.info("[SIGNUP] 회원가입 성공: userId={}, email={}", user.getId(), user.getEmail());
  }

  /**
   * 로그인
   * @param loginDto - 이메일, 비밀번호
   */
  public LoginResponseDto logIn(LoginDto loginDto) {
    log.info("[LOGIN] 로그인 시도: email={}", loginDto.getEmail());

    // 해당 이메일로 가입된 유저가 존재하는지 확인
    User user = userRepository.findByEmail(loginDto.getEmail())
        .orElseThrow(() -> {
          log.warn("[LOGIN] 존재하지 않는 사용자: email={}", loginDto.getEmail());
          return new DeepdiviewException(ErrorCode.USER_NOT_FOUND);
        });

    // 비밀번호 검증
    if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
      log.warn("[LOGIN] 비밀번호 불일치: email={}", loginDto.getEmail());
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    // 엑세스 토큰 생성
    String accessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole());

    String refreshToken = redisStringTemplate.opsForValue().get(user.getEmail());
    // 리프레시 토큰 생성 (없거나 만료되었으면 새로 생성)
    if (refreshToken == null || !jwtProvider.isTokenValid(refreshToken)) {
      refreshToken = generateAndStoreRefreshToken(user);
    } else {
      log.info("유효한 리프레시 토큰 사용");
    }

    log.info("[LOGIN] 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());
    return LoginResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .userId(user.getId())
        .email(user.getEmail())
        .nickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .role(user.getRole())
        .build();
  }

  private String generateAndStoreRefreshToken(User user) {
    String newRefreshToken = jwtProvider.generateRefreshToken(user.getEmail(), user.getRole());
    // set(키, 값, 숫자, TimeUnit)으로  일정시간(15일) 후 자동 삭제되는 토큰 저장
    redisStringTemplate.opsForValue().set(user.getEmail(), newRefreshToken, 15, TimeUnit.DAYS);
    return newRefreshToken;
  }

  /**
   * 로그아웃
   */
  @Transactional
  public void logout(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    log.info("[LOGOUT] 로그아웃 요청");

    // 현재 인증된 사용자 이메일과 요청에 포함된 이메일이 동일한지 확인
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("[LOGOUT] 존재하지 않는 사용자: email={}", email);
          return new DeepdiviewException(ErrorCode.USER_NOT_FOUND);
        });

    // 리프레시 토큰 삭제
    Boolean deletedRefreshToken = redisStringTemplate.delete(email);
    // NullPointException 방지를 위해 Boolean 객체 사용
    if (Boolean.TRUE.equals(deletedRefreshToken)) {
      log.info("[LOGOUT] 리프레시 토큰 삭제 완료: email={}", email);
    } else {
      log.warn("[LOGOUT] 리프레시 토큰 삭제 실패: email={}", email);
    }

    // 기존의 엑세스토큰은 블랙리스트로 등록
    String accessToken = jwtProvider.extractToken(request);

    if (accessToken != null) {
      // 남은 시간 = 만료시간 - 현재시간
      long remainTime = jwtProvider.getExpirationTimeFromToken(accessToken) - System.currentTimeMillis();

      if (remainTime > 0) {
        // 남은 시간동안 블랙리스트로 등록
        redisStringTemplate.opsForValue().set(accessToken, "logout", remainTime, TimeUnit.MILLISECONDS);
        log.info("[LOGOUT] 엑세스토큰 블랙리스트로 등록 완료");
      } else {
        log.info("[LOGOUT] 만료된 엑세스 토큰");
      }
    }

    // SecurityContext 초기화
    SecurityContextHolder.clearContext();
    log.info("LOGOUT SecurityContext 초기화");

    notificationService.disconnectEmitter(user.getId());
    log.info("[LOGOUT] SSE 연결 종료 : userId = {}", user.getId());

    log.info("[LOGOUT] 로그아웃 완료: userId={}, email={}", user.getId(), user.getEmail());
  }

  /**
   * 리프레시 토큰으로 엑세스 토큰 재발급
   * @param refreshToken
   */
  public TokenDto reissueAccessToken(String refreshToken) {
    log.info("[REISSUE] 리프레시 토큰으로 엑세스 토큰 재발급 요청");

    // 리프레시 토큰이 유효한지 확인
    if (!jwtProvider.isTokenValid(refreshToken)) {
      throw new DeepdiviewException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 리프레시 토큰에서 이메일 추출 후, Redis에 저장된 토큰과 일치 여부 확인
    String email = jwtProvider.extractEmail(refreshToken);
    String storedRefreshToken = redisStringTemplate.opsForValue().get(email);
    if (!refreshToken.equals(storedRefreshToken)) {
      throw new DeepdiviewException(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 사용자 정보 가져오기
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    // 새 엑세스 토큰 생성/발급
    String newAccessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRole());

    log.info("[REISSUE] 엑세스 토큰 재발급 완료");
    return new TokenDto(newAccessToken);
  }

  /**
   * 회원탈퇴
   * @param accountDeleteDto - 로그인 된 상태에서 비밀번호 입력
   */
  @Transactional
  public void deleteAccount(AccountDeleteDto accountDeleteDto, HttpServletRequest request) {

    User user = userService.getLoginUser();
    log.info("[DELETE_ACCOUNT] 회원탈퇴 요청: userId={}, email={}", user.getId(), user.getEmail());

    if (user.getRole() == Role.ADMIN) {
      log.warn("[DELETE_ACCOUNT] 관리자 탈퇴 차단");
      throw new DeepdiviewException(ErrorCode.ADMIN_CANNOT_BE_DELETED);
    }

    // 비밀번호 확인
    if (!passwordEncoder.matches(accountDeleteDto.getPassword(), user.getPassword())) {
      log.warn("[DELETE_ACCOUNT] 비밀번호 불일치: userId={}, email={}", user.getId(), user.getEmail());
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    // 기존의 엑세스토큰은 블랙리스트로 등록
    String accessToken = jwtProvider.extractToken(request);

    if (accessToken != null) {
      // 남은 시간 = 만료시간 - 현재시간
      long remainTime =
          jwtProvider.getExpirationTimeFromToken(accessToken) - System.currentTimeMillis();

      if (remainTime > 0) {
        // 남은 시간동안 블랙리스트로 등록
        redisStringTemplate.opsForValue()
            .set(accessToken, "logout", remainTime, TimeUnit.MILLISECONDS);
        log.info("[DELETE_ACCOUNT] 엑세스 토큰 블랙리스트로 등록 완료");
      }
    }

    redisStringTemplate.delete(user.getEmail());
    log.info("[DELETE_ACCOUNT] 리프레시 토큰 삭제");

    notificationService.disconnectEmitter(user.getId());
    log.info("[DELETE_ACCOUNT] SSE 연결 종료 : userId = {}", user.getId());
    // 사용자 삭제
    userRepository.delete(user);
    log.info("[DELETE_ACCOUNT] 회원탈퇴 완료: userId={}, email={}", user.getId(), user.getEmail());

    SecurityContextHolder.clearContext();
    log.info("[DELETE_ACCOUNT] SecurityContext 초기화");
  }

}
