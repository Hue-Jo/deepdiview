package community.ddv.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

  ALREADY_EXIST_MEMBER("이미 존재하는 사용자입니다.", HttpStatus.BAD_REQUEST),
  ALREADY_EXIST_NICKNAME("이미 존재하는 닉네임입니다. 다른 닉네임을 작성해주세요", HttpStatus.BAD_REQUEST),
  NOT_VALID_PASSWORD("비밀번호가 일치하지 않습니다. 비밀번호를 다시 확인해주세요", HttpStatus.BAD_REQUEST),
  NOT_ENOUGH_PASSWORD("비밀번호는 8자 이상으로 설정해야 합니다.", HttpStatus.BAD_REQUEST),
  USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
  INVALID_REFRESH_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.BAD_REQUEST),
  MOVIE_NOT_FOUND("존재하지 않는 영화입니다.", HttpStatus.NOT_FOUND),
  ALREADY_COMMITED_REVIEW("이미 해당 영화에 대한 리뷰를 작성했습니다.", HttpStatus.BAD_REQUEST),
  REVIEW_NOT_FOUND("존재하지 않는 리뷰입니다.", HttpStatus.BAD_REQUEST),
  INVALID_USER("작성자만 가능합니다.", HttpStatus.FORBIDDEN),
  COMMENT_NOT_FOUND("댓글이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
  NOT_MATCHED_CONTENT("유효하지 않은 내용입니다.", HttpStatus.BAD_REQUEST),
  UNAUTHORIZED("로그인되어 있지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
  ALREADY_EXIST_ADMIN("관리자는 한 명만 가능합니다.", HttpStatus.FORBIDDEN),
  ONLY_ADMIN_CAN("관리자만 할 수 있는 기능입니다.", HttpStatus.FORBIDDEN),
  INVALID_REQUEST("잘못된 요청입니다. 다시 확인해주세요", HttpStatus.BAD_REQUEST),
  INVALID_VOTE_CREAT_DATE("투표 생성은 일,월요일에만 가능합니다.", HttpStatus.BAD_REQUEST),
  VOTE_NOT_FOUND("존재하지 않는 투표입니다.", HttpStatus.BAD_REQUEST),
  CERTIFICATION_NOT_FOUND("인증요청이 존재하지 않습니다.", HttpStatus.BAD_REQUEST),
  ALREADY_APPROVED("이미 승인되었습니다.", HttpStatus.BAD_REQUEST
  ),;


  private final String description;
  private final HttpStatus httpStatus;
}
