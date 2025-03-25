package community.ddv.service;

import community.ddv.constant.CertificationStatus;
import community.ddv.constant.ErrorCode;
import community.ddv.constant.NotificationType;
import community.ddv.dto.NotificationDTO;
import community.ddv.dto.NotificationResponseDTO;
import community.ddv.entity.Certification;
import community.ddv.entity.Notification;
import community.ddv.entity.Review;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.CertificationRepository;
import community.ddv.repository.NotificationRepository;
import community.ddv.repository.ReviewRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

  // SSE 연결을 저장할 Map
  private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
  private final UserService userService;
  private final NotificationRepository notificationRepository;
  private final CertificationRepository certificationRepository;
  private final ReviewRepository reviewRepository;

  /**
   * SSE 구독 메서드
   * @param userId
   */
  public SseEmitter subscribe(Long userId) {

    // 로그인된 사용자만 구독을 허용
    User loggedInUser = userService.getLoginUser();  // 로그인된 사용자 확인
    if (!loggedInUser.getId().equals(userId)) {
      throw new DeepdiviewException(ErrorCode.UNAUTHORIZED);  // 예외 처리
    }

    log.info("SSE 구독 시작 : userId = {}", userId);

    // 이미 구독중이면 종료 후 제거
    SseEmitter existingEmitter = userEmitters.get(userId);
    if (existingEmitter != null) {
      log.info("기존 SSE 연결 종료");
      existingEmitter.complete();
      userEmitters.remove(userId);
    }

    // 새로운 SSE 연결
    SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 * 60 초 (타임아웃 30분)
    userEmitters.put(userId, emitter);
    log.info("새로운 SSE 연결 추가 : userId = {}", userId);

    // 연결 종료시 emitter 제거
    emitter.onCompletion(() -> {
      log.info("SSE 연결 종료 : userId = {}", userId);
      userEmitters.remove(userId);
    });

    emitter.onTimeout(() -> {
      log.warn("SSE 연결 타임아웃 : userId = {}", userId);
      userEmitters.remove(userId);
    });

    emitter.onError((e) -> {
      log.error("SSE 연결 에러 발생 : userId = {}, error = {}", userId, e.getMessage());
      userEmitters.remove(userId);
    });

    try {
      log.info("SSE 초기 메시지 전송 시도: userId = {}", userId);
      emitter.send(SseEmitter.event()
          .name("connect")
          .data("SSE 연결 성공 초기 메시지"));
    } catch (IOException e) {
      log.error("SSE 초기 메시지 전송 실패: userId = {}, error = {}", userId, e.getMessage());
      emitter.completeWithError(e);
      userEmitters.remove(userId);
    }

    return emitter;
  }


//  // 30초마다 ping 보내기
//  @Scheduled(fixedRate = 30000)
//  public void sendPingToClients() {
//
//    if (userEmitters.isEmpty()) {
//      return; // ping 보낼 구독자가 없으면 return
//    }
//
//    for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
//      Long userId = entry.getKey();
//      SseEmitter sseEmitter = entry.getValue();
//
//      if (sseEmitter == null) {
//        continue;
//      }
//
//      try {
//        sseEmitter.send(SseEmitter.event()
//            .name("ping")
//            .data("keep-alive"));
//      } catch (IOException e) {
//        sseEmitter.complete(); // 연결 종료
//        userEmitters.remove(userId); // 목록에서 제거
//      }
//    }
//  }

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
        log.info("알림 전송 성공 : userId = {}, notificationType = {}", userId, notificationDTO.getMessage());
      } catch (IOException e) {
        log.info("알림 전송 실패 : userId = {}", userId);
        emitter.completeWithError(e);
        userEmitters.remove(userId);
        log.info("SSE Emitter 제거 : userId = {}", userId);
      }
    } else {
      log.warn("SSE Emitter가 존재하지 않습니다 : userId = {}", userId);
    }
  }

  /**
   * 리뷰에 댓글이 달렸을 때의 알람
   * @param commenterId
   * @param reviewId
   */
  public void commentAdded(Long commenterId, Long reviewId) {
    log.info("댓글 추가 알림 생성 시작: reviewId = {}", reviewId);

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    User reviewer = review.getUser(); // 리뷰 작성자 (댓글 알림 받는 사람)
    log.info("리뷰 작성자 : userId = {}", reviewer.getId());

    if (commenterId.equals(reviewer.getId())) {
      log.info("자신의 리뷰에 댓글 작성 - 알림 X");
      return;
    }

    NotificationDTO notificationDTO = new NotificationDTO(
        NotificationType.COMMENT_ADDED.getMessage(), reviewId
    );

    Notification notification = Notification.builder()
        .user(reviewer)
        .notificationType(NotificationType.COMMENT_ADDED)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);

    log.info("댓글이 달렸다는 알림 전송 완료 ");
    sendNotification(reviewer.getId(), notificationDTO);
  }


  /**
   * 좋아요 알림
   * @param reviewId
   */
  public void likeAdded(Long likerId, Long reviewId) {
    log.info("좋아요 알림 생성 시작: reviewId = {}", reviewId);

    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.REVIEW_NOT_FOUND));

    User reviewer = review.getUser(); // 리뷰 작성자 (좋아요 알림 받는 사람)
    log.info("리뷰 작성자 : userId = {}", reviewer.getId());

    if (likerId.equals(reviewer.getId())) {
      log.info("자신의 리뷰에 좋아요 - 알림 X");
      return;
    }

    NotificationDTO notificationDTO = new NotificationDTO(
        NotificationType.LIKE_ADDED.getMessage(), reviewId
    );

    Notification notification = Notification.builder()
        .user(reviewer)
        .notificationType(NotificationType.LIKE_ADDED)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);
    log.info("좋아요가 달렸다는 알림 전송 완료");

    sendNotification(reviewer.getId(), notificationDTO);
  }


  /**
   * 인증상태가 변경되었을 때의 알림
   * @param certificationId
   * @param status
   */
  public void certificateResult(Long certificationId, CertificationStatus status) {
    log.info("인증 결과 알림 생성 시작");

    Certification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    User user = certification.getUser();
    log.info("인증 요청자 : userId = {}", user.getId());

    String message = "";
    if (status == CertificationStatus.APPROVED) {
      message = "인증이 승인되었습니다.";
    } else if (status == CertificationStatus.REJECTED) {
      message = "인증이 거절되었습니다.";
    }

    NotificationDTO notificationDTO = new NotificationDTO(message, certificationId);

    Notification notification = Notification.builder()
        .user(user)
        .notificationType(NotificationType.CERTIFICATION_RESULT)
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();

    notificationRepository.save(notification);
    log.info("인증 결과 알림 전송");

    sendNotification(user.getId(), notificationDTO);
  }

  // 알림 목록 조회
  public List<NotificationResponseDTO> getNotifications() {
    log.info("알림 목록 조회 요청");
    User user = userService.getLoginUser();
    log.info("요청자 : userId = {}", user.getId());
    List<Notification> notifications = notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
    return notifications.stream()
        .map(this::convertToNotificationResponseDTO)
        .collect(Collectors.toList());
  }

  private NotificationResponseDTO convertToNotificationResponseDTO(Notification notification) {
    return NotificationResponseDTO.builder()
        .notificationId(notification.getId())
        .message(notification.getNotificationType().getMessage())
        .isRead(notification.isRead())
        .createdAt(notification.getCreatedAt())
        .build();
  }

  // 알림 읽음 처리
  public void markNotificationAsRead(Long notificationId) {
    User user = userService.getLoginUser();
    log.info("알림 읽음 시도 : userId = {}", user.getId());
    Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.NOTIFICATION_NOT_FOUND));

    if (!notification.isRead()) {
      notification.markAsRead();
      notificationRepository.save(notification);;
      log.info("알림 읽음처리 완료");
    }
  }
}
