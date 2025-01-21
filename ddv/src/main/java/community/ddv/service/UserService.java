package community.ddv.service;

import community.ddv.component.JwtProvider;
import community.ddv.constant.ErrorCode;
import community.ddv.constant.Role;
import community.ddv.dto.UserDTO.AccountDeleteDto;
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

}

