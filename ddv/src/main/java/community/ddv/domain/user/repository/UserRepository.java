package community.ddv.domain.user.repository;

import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.constant.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);
  Optional<User> findByNickname(String nickname);

  boolean existsByRole(Role role);

}
