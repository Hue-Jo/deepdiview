package community.ddv.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

  // 사용자 관련 에러코드
  ALREADY_EXIST_MEMBER("이미 존재하는 사용자입니다.", HttpStatus.BAD_REQUEST),
  ALREADY_EXIST_NICKNAME("이미 존재하는 닉네임입니다. 다른 닉네임을 작성해주세요", HttpStatus.BAD_REQUEST),
  NOT_VALID_PASSWORD("비밀번호가 일치하지 않습니다. 비밀번호를 다시 확인해주세요", HttpStatus.BAD_REQUEST),
  NOT_ENOUGH_PASSWORD("비밀번호는 8자 이상으로 설정해야 합니다.", HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
  INVALID_REFRESH_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.BAD_REQUEST),
  UNAUTHORIZED("로그인되어 있지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),

  // 영화 관련 에러코드
  MOVIE_NOT_FOUND("존재하지 않는 영화입니다.", HttpStatus.NOT_FOUND),
  KEYWORD_NOT_FOUND("해당 키워드를 포함하는 영화가 존재하지 않습니다", HttpStatus.NOT_FOUND),

  // 리뷰, 댓글 관련 에러코드
  ALREADY_COMMITED_REVIEW("이미 해당 영화에 대한 리뷰를 작성했습니다. 수정만 가능합니다.", HttpStatus.BAD_REQUEST),
  REVIEW_NOT_FOUND("존재하지 않는 리뷰입니다.", HttpStatus.BAD_REQUEST),
  INVALID_USER("작성자만 가능합니다.", HttpStatus.FORBIDDEN),
  COMMENT_NOT_FOUND("댓글이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
  COMMENT_NOT_BELONG_TO_REVIEW("댓글이 해당 리뷰에 속하지 않습니다.", HttpStatus.BAD_REQUEST),

  // 관리자 관련 에러코드
  ONLY_ADMIN_CAN("관리자만 할 수 있는 기능입니다.", HttpStatus.FORBIDDEN),
  ADMIN_CANNOT_BE_DELETED("관리자 계정은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),

  // 투표 관련 에러코드
  INVALID_VOTE_CREAT_DATE("투표 생성은 일요일에만 가능합니다.", HttpStatus.BAD_REQUEST),
  VOTE_NOT_FOUND("존재하지 않는 투표입니다.", HttpStatus.BAD_REQUEST),
  AlREADY_VOTED("이미 참여한 투표입니다. 다음주에 다시 투표해주세요", HttpStatus.BAD_REQUEST),
  INVALID_VOTE_PERIOD("투표 기간이 아닙니다.", HttpStatus.BAD_REQUEST),
  ALREADY_EXIST_VOTE("이미 이번주에 생성한 투표가 있습니다.", HttpStatus.BAD_REQUEST),
  VOTE_RESULT_NOT_FOUND("투표 결과가 존재하지 않습니다.", HttpStatus.NOT_FOUND),
  MOVIE_NOT_FOUND_IN_VOTE("해당 영화가 투표 내에 존재하지 않습니다.", HttpStatus.BAD_REQUEST),

  // 파일 업로드 관련 에러코드
  IMAGE_FILE_ONLY("이미지 파일(jpg, jpeg, png, gif)만 업로드 가능합니다.", HttpStatus.BAD_REQUEST),
  FILE_SIZE_EXCEEDED("파일 크기는 5MB를 초과할 수 없습니다", HttpStatus.PAYLOAD_TOO_LARGE),
  FILE_NOT_FOUND("파일을 업로드해주세요", HttpStatus.NOT_FOUND),
  FILE_UPLOAD_FAILED("파일 업로드 중 문제가 발생했습니다", HttpStatus.BAD_REQUEST),
  FILE_DELETE_FAILED("파일 삭제 중 문제가 발생했습니다.", HttpStatus.BAD_REQUEST),

  // 인증 관련 에러코드
  CERTIFICATION_NOT_FOUND("인증요청이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
  ALREADY_APPROVED("이미 승인되었습니다.", HttpStatus.BAD_REQUEST),

  // 토론 관련 에러코드
  NOT_CERTIFIED_YET("토론 작성 권한이 없습니다. 인증을 먼저 완료해주세요", HttpStatus.FORBIDDEN),
  INVALID_REVIEW_PERIOD("토론 작성 기간이 아닙니다. 다음 주에 새로운 영화로 만나요", HttpStatus.BAD_REQUEST),

  // 알림 관련 에러코드
  NOTIFICATION_NOT_FOUND("존재하지 않는 알람입니다.", HttpStatus.NOT_FOUND);

  private final String description;
  private final HttpStatus httpStatus;
}
