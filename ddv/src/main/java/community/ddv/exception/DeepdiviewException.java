package community.ddv.exception;

import community.ddv.constant.ErrorCode;
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
