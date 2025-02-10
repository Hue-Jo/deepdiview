package community.ddv.controller;

import community.ddv.dto.UserDTO;
import community.ddv.dto.UserDTO.AccountDeleteDto;
import community.ddv.dto.UserDTO.AccountUpdateDto;
import community.ddv.dto.UserDTO.AdminDto;
import community.ddv.dto.UserDTO.LoginDto;
import community.ddv.dto.UserDTO.SignUpDto;
import community.ddv.dto.UserDTO.UserInfoDto;
import community.ddv.response.LoginResponse;
import community.ddv.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  // 회원가입 API
  @Operation(summary = "회원가입")
  @PostMapping("/signup")
  public ResponseEntity<Void> signup(@RequestBody @Valid SignUpDto signUpDto) {
    userService.signUp(signUpDto);
    return ResponseEntity.ok().build();
  }

  // 로그인 API
  @Operation(summary = "로그인")
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginDto loginDto) {
    LoginResponse loginResponse = userService.logIn(loginDto);
    return ResponseEntity.ok(loginResponse);
  }

  // 회원탈퇴 API
  @Operation(summary = "회원탈퇴")
  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteAccount(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountDeleteDto accountDeleteDto
  ) {
    String email = userDetails.getUsername();
    userService.deleteAccount(email, accountDeleteDto);
    return ResponseEntity.noContent().build();
  }

  // 회원정보 수정 API
  @Operation(summary = "회원정보 수정")
  @PutMapping("/me")
  public ResponseEntity<Void> updateAccount(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountUpdateDto accountUpdateDto
  ) {
    String email = userDetails.getUsername();
    userService.updateAccount(email, accountUpdateDto);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "한줄소개 설정/수정", description = "회원가입 직후에는 새롭게 설정, 설정된 이후에는 수정")
  @PutMapping("/me/intro")
  public ResponseEntity<Void> updateIntro(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountUpdateDto accountUpdateDto
  ) {
    String email = userDetails.getUsername();
    userService.updateOneLineIntro(email, accountUpdateDto);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "내 정보 확인", description = "닉네임, 이메일, 프로필사진, 한줄소개, 리뷰수, 댓글수")
  @GetMapping("/me")
  public ResponseEntity<UserInfoDto> getMyInfo() {
    UserInfoDto userInfoDto = userService.getMyInfo();
    return ResponseEntity.ok(userInfoDto);
  }

  @Operation(summary = "다른 유저 정보 확인", description = "닉네임, 프로필사진, 한줄소개, 리뷰수, 댓글수")
  @GetMapping("/{userId}")
  public ResponseEntity<UserInfoDto> getMyInfo(
      @PathVariable Long userId) {
    UserInfoDto userInfoDto = userService.getOthersInfo(userId);
    return ResponseEntity.ok(userInfoDto);
  }
}
