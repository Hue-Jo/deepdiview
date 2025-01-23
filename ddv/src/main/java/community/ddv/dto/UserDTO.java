package community.ddv.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class UserDTO {


  @Getter
  public static class SignUpDto {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "이메일 형식에 맞게 작성해주세요")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
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
  public static class AccountDeleteDto {
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
  }

  @Getter
  public static class AccountUpdateDto {

    private String newNickname;

    private String newPassword;
    private String newConfirmPassword;
  }


}
