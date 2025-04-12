package community.ddv.global.exception;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ErrorResponse {

  private final String errorCode;    // 에러 코드
  private final String errorMessage; // 에러 메시지
  private final Map<String, String> validErrors; // 유효성 검사 오류

  // 유효성 검사 에러 없는 기본 생성자
  public ErrorResponse(String errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.validErrors = null;
  }
}
