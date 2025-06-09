package community.ddv.domain.user.service;

import community.ddv.domain.user.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// User를 Spring Security가 인식할 수 있는 형태로 래핑하는 역할
@Getter
public class UserDetailsImpl implements UserDetails {

  private final Long id;
  private final String email;
  private final String password;

  public UserDetailsImpl(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.password = user.getPassword();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

}
