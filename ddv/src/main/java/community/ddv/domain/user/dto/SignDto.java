package community.ddv.domain.user.dto;

import community.ddv.domain.user.constant.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class SignDto {

  @Getter
  public static class SignUpDto {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "이메일 형식에 맞게 작성해주세요")
    private String email;

    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,}$",
        message = "영문 소문자와 숫자를 포함하여 8자 이상이어야 합니다."
    )
    private String password;

    @NotBlank(message = "비밀번호를 확인해주세요")
    private String confirmPassword;

    @NotBlank(message = "닉네임을 입력해주세요")
    private String nickname;

  }

  @Getter
  public static class LoginDto {

    @Email(message = "이메일 형식에 맞게 입력해주세요")
    @NotBlank(message = "이메일을 입력하세요.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class LoginResponseDto {
    private String accessToken;
    private String refreshToken;

    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;

    private Role role;
  }

  @Getter
  public static class AccountDeleteDto {
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
  }

  @Getter
  @AllArgsConstructor
  @Builder
  public static class TokenDto {
    private String newAccessToken;
  }


}
