package community.ddv.controller;

import community.ddv.dto.UserDTO.AccountDeleteDto;
import community.ddv.dto.UserDTO.AccountUpdateDto;
import community.ddv.dto.UserDTO.LoginDto;
import community.ddv.dto.UserDTO.SignUpDto;
import community.ddv.response.LoginResponse;
import community.ddv.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  @PostMapping("/signup")
  public ResponseEntity<Void> signup(@RequestBody @Valid SignUpDto signUpDto) {
    userService.signUp(signUpDto);
    return ResponseEntity.ok().build();
  }

  // 로그인 API
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginDto loginDto) {
    LoginResponse loginResponse = userService.logIn(loginDto);
    return ResponseEntity.ok(loginResponse);
  }

  // 회원탈퇴 API
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
  @PutMapping("/me")
  public ResponseEntity<Void> updateAccount(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody AccountUpdateDto accountUpdateDto
  ) {
    String email = userDetails.getUsername();
    userService.updateAccount(email, accountUpdateDto);
    return ResponseEntity.ok().build();
  }
}
