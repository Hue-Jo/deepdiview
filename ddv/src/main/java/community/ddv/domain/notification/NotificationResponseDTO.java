package community.ddv.domain.notification;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponseDTO {
  private Long notificationId;
  private NotificationType notificationType;
  private String message;
  private Long relatedId;
  private boolean isRead;
  private LocalDateTime createdAt;

}
