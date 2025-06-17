package community.ddv.domain.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);

  //Page<Notification> findByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime dayBeforeOneMonth, Pageable pageable);
  @Query("""
      SELECT n FROM Notification n
      WHERE n.user.id = :userId
      AND n.createdAt >= :fromDate
      And (
            (:cursorCreatedAt IS NULL AND :cursorId IS NULL)
            OR (n.createdAt < :cursorCreatedAt)
            OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)
      )
      ORDER by n.createdAt DESC, n.id DESC
""")
  List<Notification> findByUserIdWithCursor(
      @Param("userId") Long userId,
      @Param(("fromDate")) LocalDateTime fromDate,
      @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable
  );

  List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
  boolean existsByUser_IdAndIsReadFalse(Long userId);
  void deleteByCreatedAtBefore(LocalDateTime notificationResetDay);

}
