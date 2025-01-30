package community.ddv.component;

import community.ddv.constant.Role;
import community.ddv.entity.User;
import community.ddv.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @PostConstruct
  public void initAdmin() {
    if (userRepository.findByEmail("admin@mail.com").isEmpty()) {
      User admin = User.builder()
          .email("admin@mail.com")
          .password(passwordEncoder.encode("admin"))
          .role(Role.ADMIN)
          .build();
      userRepository.save(admin);
      log.info("관리자 계정이 생성되었습니다.");
    }
  }

}
