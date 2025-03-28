package community.ddv.controller;

import community.ddv.dto.CommentDTO.CommentRequestDto;
import community.ddv.dto.CommentDTO.CommentResponseDto;
import community.ddv.dto.MovieDTO;
import community.ddv.dto.ReviewDTO;
import community.ddv.dto.ReviewResponseDTO;
import community.ddv.service.DiscussionService;
import community.ddv.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {
  private final DiscussionService discussionService;
  private final VoteService voteService;

  @Operation(summary = "인증승인된 사용자의 토론 게시판 리뷰 작성", description = "서버에서 지난주 투표1위의 tmdbID가 자동 인식되기 때문에 RequestBody에 tmdbId는 작성하지 않아야 합니다. ")
  @PostMapping("/reviews")
  public ResponseEntity<ReviewResponseDTO> createDiscussion(
      @RequestBody @Valid ReviewDTO reviewDTO) {

    ReviewResponseDTO review = discussionService.createDiscussion(reviewDTO);
    return ResponseEntity.status(HttpStatus.CREATED).body(review);
  }

  @Operation(summary = "일요일인지 여부 T/F")
  @GetMapping("/is-sunday")
  public ResponseEntity<Boolean> isSunday() {
    return ResponseEntity.ok(discussionService.isTodaySunday());
  }

  @Operation(summary = "이번주 토론 영화(= 지난주 1위 영화) id 조회")
  @GetMapping("/this-week-movie")
  public ResponseEntity<Map<String, Long>> getThisWeekMovie() {
    Long tmdbId = voteService.getLastWeekTopVoteMovie();
    Map<String, Long> response = new HashMap<>();
    response.put("TmdbId", tmdbId);
    return ResponseEntity.ok(response);
  }
}
