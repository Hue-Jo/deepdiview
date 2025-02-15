package community.ddv.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class TokenDTO {

  @Getter
  public static class RefreshTokenDto {
    private String refreshToken;
  }

  @Getter
  @AllArgsConstructor
  public static class AccessTokenResponseDto {
    private String accessToken;
  }

}
