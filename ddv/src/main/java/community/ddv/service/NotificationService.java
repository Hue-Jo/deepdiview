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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  // SSE 연결을 저장할 Map
  private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;

  /**
   * SSE 구독 메서드
   * @param userId
   */
  public SseEmitter subscribe(Long userId) {

    log.info("SSE 구독 시작 : userId = {}", userId);
    // 이미 구독중이면 기존 emitter 반환
    if (userEmitters.containsKey(userId)) {
      //emitter.complete(); 기존에 연결된 게 있으면 종료 처리
      return userEmitters.get(userId);
    }

    SseEmitter emitter = new SseEmitter(3 * 60 * 1000L); // 3 * 60 초 (타임아웃 3분)
    log.debug("새 SSE Emitter 생성 : 타임아웃 = 3분");
    // 새 연결 저장
    userEmitters.put(userId, emitter);
    log.info("새 SSE 연결 저장");

    // 연결 종료시 emitter 제거
    emitter.onCompletion(() -> {
      log.info("SSE 연결 완료: userId = {}", userId);
      userEmitters.remove(userId);
    });
    emitter.onTimeout(() -> {
      log.info("SSE 연결 타임아웃: userId = {}", userId);
      userEmitters.remove(userId);
    });
    return emitter;
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
        log.info("알림 전송 성공 : userId = {}", userId);
      } catch (IOException e) {
        log.info("알림 전송 실패 : userId = {}", userId);
        emitter.completeWithError(e);
      }
    } else {
      log.warn("해당 사용자에게 SSE Emitter가 존재하지 않습니다.");
    }
  }

  /**
   * 리뷰에 댓글이 달렸을 때의 알람
   * @param userId
   * @param reviewId
   */
  public void commentAdded(Long userId, Long reviewId) {
    log.info("댓글 추가 알림 생성 시작: userId = {}, reviewId = {}", userId, reviewId);
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

  /**
   * 인증상태가 변경되었을 때의 알림
   * @param userId
   * @param status
   */
  public void certificateResult(Long userId, CertificationStatus status) {
    log.info("인증 결과 알림 생성 시작: userId = {}, status = {}", userId, status);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.USER_NOT_FOUND));

    String message = "";
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
