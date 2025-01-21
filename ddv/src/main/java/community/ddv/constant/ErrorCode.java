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
  USER_NOT_FOUND("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
  INVALID_REFRESH_TOKEN("유효하지 않은 토큰입니다.", HttpStatus.BAD_REQUEST);

  private final String description;
  private final HttpStatus httpStatus;
}
