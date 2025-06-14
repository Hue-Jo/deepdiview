package community.ddv.domain.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);
  Page<Notification> findByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime dayBeforeOneMonth, Pageable pageable);
  List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
  boolean existsByUser_IdAndIsReadFalse(Long userId);
  void deleteByCreatedAtBefore(LocalDateTime notificationResetDay);

}
