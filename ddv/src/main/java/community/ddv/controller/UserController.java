package community.ddv.controller;

import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.dto.UserDTO.AccountDeleteDto;
import community.ddv.dto.UserDTO.AccountUpdateDto;
import community.ddv.dto.UserDTO.LoginDto;
import community.ddv.dto.UserDTO.OneLineIntro;
import community.ddv.dto.UserDTO.SignUpDto;
import community.ddv.dto.UserDTO.TokenDto;
import community.ddv.dto.UserDTO.UserInfoDto;
import community.ddv.response.LoginResponse;
import community.ddv.service.CommentService;
import community.ddv.service.ReviewService;
import community.ddv.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  @Operation(summary = "로그아웃")
  @DeleteMapping("/logout")
  public ResponseEntity<Void> logout() {
    userService.logout();
    return ResponseEntity.noContent().build();
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

  @Operation(summary = "리프레시 토큰으로 엑세스 토큰 재발급")
  @PostMapping("/reissue-access-token")
  public ResponseEntity<TokenDto> reissueAccessToken(@RequestHeader("Authorization") String authorization) {
      // Authorization 헤더에서 'Bearer '를 제외한 리프레시 토큰 추출
    String refreshToken = authorization.replace("Bearer ", "");

      // 엑세스 토큰 재발급
    TokenDto newAccessToken = userService.reissueAccessToken(refreshToken);
    return ResponseEntity.ok(newAccessToken);
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
      @RequestBody OneLineIntro oneLineIntro
  ) {
    userService.updateOneLineIntro(oneLineIntro);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "내 정보 확인", description = "닉네임, 이메일, 프로필사진, 한줄소개, 리뷰수, 댓글수, 별점 분포")
  @GetMapping("/me")
  public ResponseEntity<UserInfoDto> getMyInfo() {
    UserInfoDto userInfoDto = userService.getMyInfo();
    return ResponseEntity.ok(userInfoDto);
  }

  @Operation(summary = "다른 유저 정보 확인", description = "닉네임, 프로필사진, 한줄소개, 리뷰수, 댓글수, 별점 분포")
  @GetMapping("/{userId}")
  public ResponseEntity<UserInfoDto> getOthersInfo(
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
      @RequestParam(value = "certifiedFilter", required = false, defaultValue = "false") Boolean certifiedFilter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<ReviewResponseDTO> reviews = reviewService.getReviewsByUserId(userId, pageable, certifiedFilter);
    return ResponseEntity.ok(reviews);
  }

  @Operation(summary = "프로필사진 업로드/수정", description = "프로필 사진이 존재하는 경우 수정, 존재하지 않는 경우 새롭게 업로드 됩니다.")
  @PutMapping("/profile-image")
  public ResponseEntity<Map<String, String>> uploadProfileImage(
      @RequestParam("file") MultipartFile file) throws IOException {
    String profileImageUrl = userService.updateProfileImage(file);
    Map<String, String> profileResponse = new HashMap<>();
    profileResponse.put("profileImage Url", profileImageUrl);
    return ResponseEntity.ok(profileResponse);
  }

  @Operation(summary = "프로필사진 삭제")
  @DeleteMapping("/profile-image")
  public ResponseEntity<Void> deleteProfileImage() throws IOException {
    userService.deleteProfileImage();
    return ResponseEntity.noContent().build();
  }

}
