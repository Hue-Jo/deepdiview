package community.ddv.constant;

import lombok.Getter;

@Getter
public enum NotificationType {

  COMMENT_ADDED("내 리뷰에 새 댓글이 달렸습니다."),
  CERTIFICATION_RESULT("인증 결과를 확인하세요.");

  private final String message;

  NotificationType(String message) {
    this.message = message;
  }

}
