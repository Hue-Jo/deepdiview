package community.ddv.domain.board.controller;

import community.ddv.domain.board.dto.ReviewDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.service.LikeService;
import community.ddv.domain.board.service.ReviewService;
import community.ddv.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
@Tag(name = "Review", description = "리뷰 관련 API에 대한 명세를 제공합니다.")
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
      @RequestBody @Valid ReviewDTO.ReviewUpdateDTO reviewUpdateDTO
  ) {
    return ResponseEntity.ok(reviewService.updateReview(reviewId, reviewUpdateDTO));
  }

  @Operation(summary = "특정 영화에 대한 리뷰 조회")
  @GetMapping("/movie/{tmdbId}")
  public ResponseEntity<PageResponse<ReviewResponseDTO>> getReviewsByMovieId(
      @PathVariable Long tmdbId,
      @RequestParam(value = "certifiedFilter", required = false, defaultValue = "false") Boolean certifiedFilter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC) Pageable pageable
  ) {
    List<Sort.Order> orders = new ArrayList<>(pageable.getSort().toList());
    if (orders.stream().anyMatch(order -> order.getProperty().equals("likeCount"))) {
      boolean hasCreatedAt = orders.stream().anyMatch(order -> order.getProperty().equals("createdAt"));
      if (!hasCreatedAt) {
        orders.add(Sort.Order.desc("createdAt"));
      }
    }
    Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    Page<ReviewResponseDTO> reviews = reviewService.getReviewByMovieId(tmdbId, newPageable, certifiedFilter);
    return ResponseEntity.ok(new PageResponse<>(reviews));
  }

  @Operation(summary = "특정 리뷰 조회")
  @GetMapping("/{reviewId}")
  public ResponseEntity<ReviewResponseDTO> getReviewById(
      @PathVariable Long reviewId) {
    return ResponseEntity.ok(reviewService.getReviewById(reviewId));
  }

  @Operation(summary = "최신 리뷰 조회", description = "디폴트 사이즈는 9입니다.")
  @GetMapping("/latest")
  public ResponseEntity<PageResponse<ReviewResponseDTO>> getLatestReviews(
      @PageableDefault(size = 9, sort = "createdAt", direction = Direction.DESC) Pageable pageable
  ) {
    PageResponse<ReviewResponseDTO> response = reviewService.getLatestReviews(pageable);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "좋아요", description = "토글형식입니다. 두 번 누를 시 좋아요 취소")
  @PostMapping("/like/{reviewId}")
  public ResponseEntity<Void> toggleLike(
      @PathVariable Long reviewId) {
    likeService.toggleLike(reviewId);
    return ResponseEntity.noContent().build();
  }
}
