package community.ddv.global.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.View;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

  private final View error;

  @ExceptionHandler(DeepdiviewException.class)
  public ResponseEntity<ErrorResponse> handleDeepdiviewException(DeepdiviewException e) {

    ErrorCode errorCode = e.getErrorCode();
    ErrorResponse errorResponse = new ErrorResponse(
        errorCode.name(),
        e.getErrorMessage()
    );

    HttpStatus httpStatus = errorCode.getHttpStatus();
    return ResponseEntity.status(httpStatus).body(errorResponse);
  }

  // 유효성 검사 예외 메시지 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
    Map<String, String> fieldErrors = new HashMap<>();

    for (FieldError error : e.getBindingResult().getFieldErrors()) {
      fieldErrors.put(error.getField(), error.getDefaultMessage());
    }

    ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
    ErrorResponse errorResponse = new ErrorResponse(
        errorCode.name(),
        errorCode.getDescription(),
        fieldErrors
    );
    return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
  }

  // 타입 유형 예외 처리
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
    ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
    ErrorResponse errorResponse = new ErrorResponse(errorCode.name(), errorCode.getDescription());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex) {
    ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED;
    ErrorResponse errorResponse = new ErrorResponse(errorCode.name(), errorCode.getDescription());
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(errorResponse);
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public void handleAsyncRequestTimeoutException() {
    log.debug("[SSE] 클라이언트 연결 종료");
  }
}
