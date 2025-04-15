package community.ddv.domain.user.dto;

import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserInfoDto {


  @Getter
  public static class NicknameUpdateRequestDto {
    @NotBlank(message = "새로운 닉네임을 입력해주세요")
    private String newNickname;
  }

  @Getter
  @AllArgsConstructor
  public static class NicknameUpdateResponseDto {
    private String updatedNickname;
  }


  @Getter
  public static class PasswordUpdateDto {

    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*\\d)[a-z\\d]{8,}$",
        message = "영문 소문자와 숫자를 포함하여 8자 이상이어야 합니다."
    )
    private String newPassword;

    @NotBlank(message = "비밀번호를 확인해주세요")
    private String newConfirmPassword;
  }


  @Getter
  @NoArgsConstructor
  public static class OneLineIntroRequestDto {

    @Size(max = 50, message = "최대 50자까지만 작성 가능합니다.")
    private String oneLineIntro;


  }

  @Getter
  @AllArgsConstructor
  public static class OneLineIntroResponseDto {
    private String updatedOneLineIntro;
  }


  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class UserInfoResponseDto {

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
