package community.ddv.controller;

import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.dto.UserDTO.AccountDeleteDto;
import community.ddv.dto.UserDTO.AccountUpdateDto;
import community.ddv.dto.UserDTO.LoginDto;
import community.ddv.dto.UserDTO.SignUpDto;
import community.ddv.dto.UserDTO.UserInfoDto;
import community.ddv.response.LoginResponse;
import community.ddv.service.CommentService;
import community.ddv.service.ReviewService;
import community.ddv.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
  private final CommentService commentService;
  private final ReviewService reviewService;

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
      @RequestBody AccountDeleteDto accountDeleteDto
  ) {
    userService.deleteAccount(accountDeleteDto);
    return ResponseEntity.noContent().build();
  }

  // 회원정보 수정 API
  @Operation(summary = "회원정보 수정")
  @PutMapping("/me")
  public ResponseEntity<Void> updateAccount(
      @RequestBody AccountUpdateDto accountUpdateDto
  ) {
    userService.updateAccount(accountUpdateDto);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "한줄소개 설정/수정", description = "회원가입 직후에는 새롭게 설정, 설정된 이후에는 수정")
  @PutMapping("/me/intro")
  public ResponseEntity<Void> updateIntro(
      @RequestBody AccountUpdateDto accountUpdateDto
  ) {
    userService.updateOneLineIntro(accountUpdateDto);
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

  @Operation(summary = "특정 사용자가 작성한 댓글 조회")
  @GetMapping("/{userId}/comments")
  public ResponseEntity<Page<CommentResponseDto>> getCommentsByUserId(
      @PathVariable Long userId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<CommentResponseDto> comments = commentService.getCommentsByUserId(userId, pageable);
    return ResponseEntity.ok(comments);
  }

  @Operation(summary = "특정 사용자가 작성한 리뷰 조회")
  @GetMapping("{userId}/reviews")
  public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByUserId(
      @PathVariable Long userId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<ReviewResponseDTO> reviews = reviewService.getReviewsByUserId(userId, pageable);
    return ResponseEntity.ok(reviews);
  }

}
