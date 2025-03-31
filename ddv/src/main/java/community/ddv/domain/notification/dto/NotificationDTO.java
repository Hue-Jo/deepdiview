package community.ddv.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NotificationDTO {

  private Long notificationId;
  private String type;
  private String message;
  private Long relatedId; // 댓글이 달린 리뷰 ID , 인증 ID

}
