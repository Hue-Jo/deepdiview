package community.ddv.domain.board.custom;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RatingValidator implements ConstraintValidator<RatingValid, Double> {

  @Override
  public boolean isValid(Double rating, ConstraintValidatorContext context) {
    if (rating == null) {
      return true;
    }
    return rating >= 0.5 && rating <= 5.0 && (rating * 10) % 5 == 0;
  }
}
