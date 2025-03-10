package community.ddv.repository;

import community.ddv.constant.Role;
import community.ddv.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);
  Optional<User> findByNickname(String nickname);

  boolean existsByRole(Role role);

}
