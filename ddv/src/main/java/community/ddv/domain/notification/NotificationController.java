package community.ddv.domain.notification;

import community.ddv.domain.notification.dto.NotificationResponseDTO;
import community.ddv.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

  // 클라이언트가 SSE 연결 구독
  @Operation(summary = "SSE 연결 구독")
  @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@RequestParam Long userId) {

    return notificationService.subscribe(userId);
  }

  @Operation(summary = "알림 목록 조회")
  @GetMapping
  public ResponseEntity<PageResponse<NotificationResponseDTO>> getNotifications(
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    PageResponse<NotificationResponseDTO> notifications = notificationService.getNotifications(pageable);
    return ResponseEntity.ok(notifications);
  }

  @Operation(summary = "특정 알림 읽음처리")
  @PutMapping("/{notificationId}/read")
  public ResponseEntity<Void> markNotificationAsRead(
      @PathVariable Long notificationId) {
    notificationService.markNotificationAsRead(notificationId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "전체 알림 읽음처리")
  @PutMapping("/read-all")
  public ResponseEntity<Void> markAllNotificationAsRead() {
    notificationService.markAllNotificationAsRead();
    return ResponseEntity.noContent().build();
  }

}
