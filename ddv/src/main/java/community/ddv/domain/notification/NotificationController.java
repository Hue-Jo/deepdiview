package community.ddv.domain.notification;

import community.ddv.domain.notification.dto.NotificationResponseDTO;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 관련 API에 대한 명세를 제공합니다.")
public class NotificationController {

  private final NotificationService notificationService;
  private final UserService userService;

  // 클라이언트가 SSE 연결 구독
  @Operation(summary = "SSE 연결 구독")
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe() {
    Long userId = userService.getLoginUser().getId();
    return notificationService.subscribe(userId);
  }

  @Operation(summary = "알림 목록 조회")
  @GetMapping
  public ResponseEntity<CursorPageResponse<NotificationResponseDTO>> getNotifications(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
      @RequestParam(required = false) Long notificationId,
      @RequestParam(defaultValue = "10") int size) {

    CursorPageResponse<NotificationResponseDTO> response =
        notificationService.getNotifications(createdAt, notificationId, size);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "특정 알림 읽음처리")
  @PutMapping("/{notificationId}/read")
  public ResponseEntity<Map<String, Boolean>> markNotificationAsRead(
      @PathVariable Long notificationId) {
    boolean hasUnread = notificationService.markNotificationAsRead(notificationId);
    Map<String, Boolean> response = Map.of("hasUnread", hasUnread);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "전체 알림 읽음처리")
  @PutMapping("/read-all")
  public ResponseEntity<Void> markAllNotificationAsRead() {
    notificationService.markAllNotificationAsRead();
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "안 읽은 알림 여부 확인용")
  @GetMapping("/unread-exists")
  public ResponseEntity<Map<String, Boolean>> hasUnreadNotifications() {
    boolean hasUnread = notificationService.isNotReadNotification();
    Map<String, Boolean> response = Map.of("hasUnread", hasUnread);
    return ResponseEntity.ok(response);
  }

}
