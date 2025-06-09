package community.ddv.domain.user.dto;

import lombok.Getter;

public class EmailDto {

  @Getter
  public static class EmailRequest {
    private String email;
  }

  @Getter
  public static class EmailVerifyRequest {
    private String email;
    private String code;
  }

}
