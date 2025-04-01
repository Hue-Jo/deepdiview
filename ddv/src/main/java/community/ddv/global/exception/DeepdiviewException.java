package community.ddv.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeepdiviewException extends RuntimeException {

  private ErrorCode errorCode;
  private String errorMessage;

  public DeepdiviewException(ErrorCode errorCode) {
    this.errorCode = errorCode;
    this.errorMessage = errorCode.getDescription();
  }

}
