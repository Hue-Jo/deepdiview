package community.ddv.service;

import community.ddv.component.JwtProvider;
import community.ddv.constant.ErrorCode;
import community.ddv.constant.Role;
import community.ddv.dto.UserDTO.AccountDeleteDto;
import community.ddv.dto.UserDTO.AccountUpdateDto;
import community.ddv.dto.UserDTO.LoginDto;
import community.ddv.dto.UserDTO.SignUpDto;
import community.ddv.entity.RefreshToken;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.RefreshTokenRepository;
import community.ddv.repository.UserRepository;
import community.ddv.response.LoginResponse;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  /**
   * 회원가입
   * @param signUpDto - 이메일(중복 불가), 비밀번호, 비밀번호 확인, 닉네임(중복 불가)
   */
  public void signUp(SignUpDto signUpDto) {
    log.info("회원가입 시도 : {}", signUpDto);

    // 같은 이메일로 중복 회원가입 불가
    if (userRepository.findByEmail(signUpDto.getEmail()).isPresent()) {
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_MEMBER);
    }
    // 비밀번호 확인 로직 통과 여부
    if (!signUpDto.getPassword().equals(signUpDto.getConfirmPassword())) {
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }
    // 중복 닉네임 사용불가
    if (userRepository.findByNickname(signUpDto.getNickname()).isPresent()) {
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_NICKNAME);
    }

    User user = User.builder()
        .email(signUpDto.getEmail())
        .password(passwordEncoder.encode(signUpDto.getPassword()))
        .nickname(signUpDto.getNickname())
        .role(Role.USER) // 일반 유저를 기본 역할로 설정
        .createdAt(LocalDateTime.now())
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
    String accessToken = jwtProvider.generateAccessToken(user.getEmail());

    // 기존 리프레시 토큰 조회 -> 유효한 경우 기존 토큰 사용, 만료된 경우나 없는 경우 새로 생성
    String refreshToken;
    Optional<RefreshToken> existingRefreshToken = refreshTokenRepository.findByUserEmail(
        user.getEmail());
    if (existingRefreshToken.isPresent()) {
      RefreshToken token = existingRefreshToken.get();
      // 기존 리프레시 토큰이 만료된 경우, 새로 생성
      if (jwtProvider.isTokenExpired(token.getRefreshToken())) {
        refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
        token.builder().refreshToken(refreshToken).build();
        refreshTokenRepository.save(token);
      } else {
        // 만료가 안 된 경우, 기존 리프레시 토큰 사용
        refreshToken = token.getRefreshToken();
      }
    } else {
      refreshToken = jwtProvider.generateRefreshToken(user.getEmail());
      RefreshToken token = RefreshToken.builder()
          .refreshToken(refreshToken)
          .user(user)
          .build();
      refreshTokenRepository.save(token);
    }
    log.info("로그인 성공");
    return new LoginResponse(
        accessToken,
        refreshToken,
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getProfileImageUrl());
  }


  /**
   * 회원탈퇴
   * @param accountDeleteDto - 로그인 된 상태에서 비밀번호 입력
   */
  @Transactional
  public void deleteAccount(String email, AccountDeleteDto accountDeleteDto) {

    log.info("회원탈퇴 요청 : {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    // 비밀번호 확인
    if (!passwordEncoder.matches(accountDeleteDto.getPassword(), user.getPassword())) {
      throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
    }

    refreshTokenRepository.deleteByUser(user);
    log.info("리프레시 토큰 삭제");

    // 사용자 삭제
    userRepository.delete(user);
    log.info("회원탈퇴 완료");
  }

  /**
   * 회원정보 수정 (닉네임, 비밀번호)
   * @param accountUpdateDto
   */
  @Transactional
  public void updateAccount(String email, AccountUpdateDto accountUpdateDto) {
    log.info("회원정보 수정시도 : {}", email);

    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

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
      user.setNickname(newNickname);
      log.info("닉네임 변경성공");
    }

    if (!newPassword.isBlank()) {
      log.info("비밀번호 변경시도");

      if (!isValidPassword(newPassword)) {
        log.info("8자 이상 입력하지 않았기 때문에 비밀번호 변경 실패");
        throw new DeepdiviewException(ErrorCode.NOT_ENOUGH_PASSWORD);
      } else {
        if (!newPassword.equals(newConfirmPassword)) {
          log.info("비밀번호 확인 번호와 일치 하지 않기 떄문에 비밀번호 변경 실패");
          throw new DeepdiviewException(ErrorCode.NOT_VALID_PASSWORD);
        }
      }

      user.setPassword(passwordEncoder.encode(newPassword));
      log.info("비밀번호 변경성공");
    }

    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
    log.info("회원정보 수정완료");


  }

  // 비밀번호 8자 이상 작성해야 유효
  private boolean isValidPassword(String password) {
    String regex = "\\w{8,}";
    return Pattern.compile(regex).matcher(password).matches();
  }
}

