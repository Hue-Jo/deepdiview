package community.ddv.controller;

import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewDTO.ReviewUpdateDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.service.LikeService;
import community.ddv.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;
  private final LikeService likeService;

  @Operation(summary = "리뷰글 작성")
  @PostMapping
  public ResponseEntity<ReviewResponseDTO> createReview(
      @RequestBody @Valid ReviewDTO reviewDTO) {

    ReviewResponseDTO response = reviewService.createReview(reviewDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "리뷰글 삭제")
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable Long reviewId
  ) {
    reviewService.deleteReview(reviewId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰글 수정", description = "제목 내용 별점 각각 수정 가능, 별점은 변경하지 않을 시 null로 들어가야 함")
  @PutMapping("/{reviewId}")
  public ResponseEntity<ReviewResponseDTO> updateReview(
      @PathVariable Long reviewId,
      @RequestBody @Valid ReviewUpdateDTO reviewUpdateDTO
  ) {
    ReviewResponseDTO response = reviewService.updateReview(reviewId, reviewUpdateDTO);
    return ResponseEntity.ok(response);

  }

  @Operation(summary = "특정 영화에 대한 리뷰 조회", description = "댓글은 포함되어있지 않습니다.")
  @GetMapping("/movie/{tmdbId}")
  public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByMovieId(
      @PathVariable Long tmdbId,
      @RequestParam(value = "certifiedFilter", required = false, defaultValue = "false") Boolean certifiedFilter,
      @PageableDefault(size = 20) Pageable pageable,
      @RequestParam(value = "sortBy", required = false, defaultValue = "createdAt") String sortBy,
      @RequestParam(value = "direction", required = false, defaultValue = "DESC") Direction direction
  ) {

    Sort sort = "likeCount".equals(sortBy)
      // 좋아요순으로 정렬 시 동점일 경우 최신순 정렬
      ? Sort.by(Sort.Order.by("likeCount").with(direction))
            .and(Sort.by(Sort.Order.by("createdAt").with(direction)))
      : Sort.by(Sort.Order.by("createdAt").with(direction));

    Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

    Page<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(tmdbId, sortedPageable, certifiedFilter);
    return ResponseEntity.ok(reviews);
  }

  @Operation(summary = "특정 리뷰 조회", description = "댓글이 포함되어 있습니다.")
  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewResponseDTO> getReviewById(
      @PathVariable Long reviewId) {
    ReviewResponseDTO reviews = reviewService.getReviewById(reviewId);
    return ResponseEntity.ok(reviews);
  }

  @Operation(summary = "최신 리뷰 3개 조회")
  @GetMapping("/latest")
  public ResponseEntity<List<ReviewResponseDTO>> getLatestReviews() {
    return ResponseEntity.ok(reviewService.getLatestReviews());
  }

  @Operation(summary = "좋아요", description = "토글형식입니다. 두 번 누를 시 좋아요 취소")
  @PostMapping("/like/{reviewId}")
  public ResponseEntity<Void> toggleLike(
      @PathVariable Long reviewId) {
    likeService.toggleLike(reviewId);
    return ResponseEntity.ok().build();
  }
}
