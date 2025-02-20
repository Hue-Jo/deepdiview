package community.ddv.controller;

import community.ddv.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  // 클라이언트가 SSE 연결 구독
  @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public ResponseEntity<SseEmitter> subscribe(@PathVariable Long userId) {
    SseEmitter emitter = new SseEmitter(300_000L); // 타임아웃 5분

    notificationService.subscribe(userId, emitter);
    return ResponseEntity.ok(emitter);
  }

  // 리뷰에 댓글 달렸을 시 알림 전송
  @PostMapping("/comment/{userId}/{reviewId}")
  public ResponseEntity<Void> commentNotification(@PathVariable Long userId, @PathVariable Long reviewId) {
    notificationService.commentAdded(userId, reviewId);
    return ResponseEntity.ok().build();
  }

  // 인증 승인/거절 시 알림 전송
  @PostMapping("certification/{userId}")
  public ResponseEntity<Void> certificationNotification(@PathVariable Long userId) {
    notificationService.certificateResult(userId);
    return ResponseEntity.ok().build();
  }

}
