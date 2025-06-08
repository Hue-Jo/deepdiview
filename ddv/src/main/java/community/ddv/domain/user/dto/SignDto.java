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
        regexp = "^(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,16}$",
        message = "영문 소문자와 숫자만을 포함하여 8자 이상 16자 이하여야 합니다."
    )
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    @NotBlank(message = "비밀번호를 확인해주세요")
    private String confirmPassword;

    @Pattern(
        regexp = "^(?!\\s)(?!.*\\s{2,})[\\S\\s]{2,10}$",
        message = "닉네임은 2자 이상 10자 이하이며, 첫 글자 공백와 연속된 공백은 허용되지 않습니다."
    )
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
