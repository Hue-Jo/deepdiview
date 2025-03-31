package community.ddv.domain.notification.dto;

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
  private String message;
  private boolean isRead;
  private LocalDateTime createdAt;

}
