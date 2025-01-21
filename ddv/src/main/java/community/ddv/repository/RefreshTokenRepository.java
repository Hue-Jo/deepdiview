package community.ddv.repository;

import community.ddv.entity.RefreshToken;
import community.ddv.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByUserEmail(String email);
  Optional<RefreshToken> findByRefreshToken(String refreshToken);
  void deleteByUser(User user);
}
