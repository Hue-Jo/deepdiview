package community.ddv.constant;

public enum NotificationType {

  COMMENT_ADDED("내 리뷰에 새 댓글이 달렸습니다."),
  CERTIFICATION_APPROVED("인증이 승인되었습니다."),
  CERTIFICATION_REJECTED("인증이 거절되었습니다.");

  private final String message;

  NotificationType(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
