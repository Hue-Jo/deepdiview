package community.ddv.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ErrorResponse {

  private final String errorCode;    // 에러 코드
  private final String errorMessage; // 에러 메시지

}
