package community.ddv.repository;

import community.ddv.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);
  List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

}
