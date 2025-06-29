package community.ddv.domain.notification;

import community.ddv.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  private User user;

  @Enumerated(EnumType.STRING)
  private NotificationType notificationType; // 알림 타입/메시지

  private Long relatedId; // 알림대상(리뷰, 인증 id)

  private boolean isRead; // 확인 여부

  public void markAsRead() {
    isRead = true;
  }

  private LocalDateTime createdAt;

}
