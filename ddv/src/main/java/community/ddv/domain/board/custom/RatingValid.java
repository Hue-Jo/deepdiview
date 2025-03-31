package community.ddv.domain.board.custom;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = RatingValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RatingValid {
  String message() default "별점은 0.5부터 5.0까지입니다. 0.5단위로만 줄 수 있습니다.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

}
