package community.ddv.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class NotificationDTO {

  private String message;
  private LocalDateTime createdAt;



}
