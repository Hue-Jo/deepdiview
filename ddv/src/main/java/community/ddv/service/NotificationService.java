package community.ddv.service;

import community.ddv.constant.CertificationStatus;
import community.ddv.constant.ErrorCode;
import community.ddv.constant.NotificationType;
import community.ddv.dto.NotificationDTO;
import community.ddv.entity.Notification;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.NotificationRepository;
import community.ddv.repository.UserRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationService {

  // SSE 연결을 저장할 Map
  private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
  private final UserService userService;
  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;

  /**
   * SSE 구독 메서드
   * @param userId
   * @param emitter
   */
  public void subscribe(Long userId, SseEmitter emitter) {

    // 사용자에게 하나의 SSE 연결만 허용
    if (userEmitters.containsKey(userId)) {
      emitter.complete(); // 기존에 연결된 게 있으면 종료 처리
    }

    // 새 연결 저장
    userEmitters.put(userId, emitter);

    // 연결 종료시 emitter 제거
    emitter.onCompletion(() -> userEmitters.remove(userId));
    emitter.onTimeout(() -> userEmitters.remove(userId));
  }

  /**
   * 알림 전송 메서드
   * @param userId
   * @param notificationDTO
   */
  public void sendNotification(Long userId, NotificationDTO notificationDTO) {
    SseEmitter emitter = userEmitters.get(userId);

    if (emitter != null) {
      try {
        emitter.send(notificationDTO);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    }
  }

  /**
   * 리뷰에 댓글이 달렸을 때의 알람
   * @param userId
   * @param reviewId
   */
  public void commentAdded(Long userId, Long reviewId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    NotificationDTO notificationDTO = new NotificationDTO(
        NotificationType.COMMENT_ADDED.getMessage(), reviewId
    );

    Notification notification = Notification.builder()
        .user(user)
        .notificationType(NotificationType.COMMENT_ADDED)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);

    sendNotification(userId, notificationDTO);
  }

  public void certificateResult(Long userId, CertificationStatus status) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    String message ="";
    if (status == CertificationStatus.APPROVED) {
      message = "인증이 승인되었습니다.";
    } else if (status == CertificationStatus.REJECTED) {
      message = "인증이 거절되었습니다.";
    }

    NotificationDTO notificationDTO = new NotificationDTO(message, userId);

    Notification notification = Notification.builder()
        .user(user)
        .notificationType(NotificationType.CERTIFICATION_RESULT)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);
    sendNotification(userId, notificationDTO);
  }
}
