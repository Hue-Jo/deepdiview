package community.ddv.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

  @Getter
  @AllArgsConstructor
  public static class EmailVerifyResponse {
    private boolean success;
  }
}
