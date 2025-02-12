package community.ddv.controller;

import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.service.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {
  private final DiscussionService discussionService;

  @Operation(summary = "인증승인된 사용자의 토론 게시판 리뷰 작성")
  @PostMapping
  public ResponseEntity<ReviewResponseDTO> createDiscussion(
      @RequestBody @Valid ReviewDTO reviewDTO) {

    ReviewResponseDTO review = discussionService.createDiscussion( reviewDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(review);

  }

}
