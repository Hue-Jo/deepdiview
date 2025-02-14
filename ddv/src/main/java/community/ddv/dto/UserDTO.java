package community.ddv.dto;

import community.ddv.constant.CertificationStatus;
import community.ddv.constant.RejectionReason;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  @Getter
  public static class OneLineIntro {
    private String oneLineIntro;
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class UserInfoDto {

    private String nickname;
    private String email;
    private String profileImageUrl;
    private String oneLineIntro;
    private int reviewCount; // 리뷰 작성 개수
    private int commentCount; // 댓글 작성 개수
    private Map<Double, Long> ratingDistribution; // 별점 분포
    private CertificationStatus certificationStatus; // 인증 상태
    private RejectionReason rejectionReason; // 인증 거절 사유
  }


}
