package community.ddv.controller;

import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "리뷰글 작성")
  @PostMapping
  public ResponseEntity<ReviewResponseDTO> createReview(
      @AuthenticationPrincipal UserDetails userDetails,
      @RequestBody @Valid ReviewDTO reviewDTO) {

    String email = userDetails.getUsername();
    ReviewResponseDTO response = reviewService.createReview(email, reviewDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "리뷰글 삭제")
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long reviewId
  ) {
    String email = userDetails.getUsername();
    reviewService.deleteReview(email, reviewId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰글 수정")
  @PutMapping("/{reviewId}")
  public ResponseEntity<ReviewResponseDTO> updateReview(
      @AuthenticationPrincipal UserDetails userDetails,
      @PathVariable Long reviewId,
      @RequestBody @Valid ReviewUpdateDTO reviewUpdateDTO
  ) {
    String email = userDetails.getUsername();
    ReviewResponseDTO response = reviewService.updateReview(email, reviewId, reviewUpdateDTO);
    return ResponseEntity.ok(response);

  }

  @Operation(summary = "특정 영화에 대한 리뷰 리스트 조회")
  @GetMapping("/movie/{tmdbId}")
  public ResponseEntity<List<ReviewResponseDTO>> getReviewsByMovieId(
      @PathVariable Long tmdbId) {
    List<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(tmdbId);
    return ResponseEntity.ok(reviews);
  }


}
