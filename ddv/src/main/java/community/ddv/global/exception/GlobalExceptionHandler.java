package community.ddv.global.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {

  @ExceptionHandler(DeepdiviewException.class)
  public ResponseEntity<ErrorResponse> handleDeepdiviewException(DeepdiviewException e) {

    // 에러 코드
    ErrorCode errorCode = e.getErrorCode();

    // 에러 응답
    ErrorResponse errorResponse = new ErrorResponse(
        errorCode.name(), e.getErrorMessage()
    );

    // HttpStatus
    HttpStatus httpStatus = errorCode.getHttpStatus();

    return ResponseEntity.status(httpStatus).body(errorResponse);
  }

  // 유효성 검사 예외 메시지 처리
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });
    return ResponseEntity.badRequest().body(errors);
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
}
