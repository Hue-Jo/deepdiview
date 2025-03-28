package community.ddv.controller;

import community.ddv.dto.NotificationResponseDTO;
import community.ddv.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class NotificationController {

  private final NotificationService notificationService;

  // 클라이언트가 SSE 연결 구독
  @Operation(summary = "SSE 연결 구독")
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@RequestParam Long userId) {

    return notificationService.subscribe(userId);
  }

  @Operation(summary = "알림 목록 조회")
  @GetMapping
  public ResponseEntity<List<NotificationResponseDTO>> getNotifications() {
    List<NotificationResponseDTO> notifications = notificationService.getNotifications();
    return ResponseEntity.ok(notifications);
  }

  @Operation(summary = "특정 알림 읽음처리")
  @PutMapping("/{notificationId}/read")
  public ResponseEntity<Void> markNotificationAsRead(
      @PathVariable Long notificationId) {
    notificationService.markNotificationAsRead(notificationId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "전체 알림 읽음처리")
  @PutMapping("/read-all")
  public ResponseEntity<Void> markAllNotificationAsRead() {
    notificationService.markAllNotificationAsRead();
    return ResponseEntity.ok().build();
  }

}
