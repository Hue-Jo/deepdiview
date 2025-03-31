package community.ddv.domain.user.dto;

import community.ddv.global.constant.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class LoginResponse {

  private String accessToken;
  private String refreshToken;

  private Long userId;
  private String email;
  private String nickname;
  private String profileImageUrl;

  private Role role;
}
