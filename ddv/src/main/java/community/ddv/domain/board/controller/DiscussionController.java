package community.ddv.domain.board.controller;

import community.ddv.domain.board.dto.ReviewDTO;
import community.ddv.domain.board.dto.ReviewResponseDTO;
import community.ddv.domain.board.service.DiscussionService;
import community.ddv.domain.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
@Tag(name = "Discussion", description = "토론 관련 API에 대한 명세를 제공합니다.")
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
  public ResponseEntity<Map<String, Boolean>> isSunday() {
    boolean isSunday = discussionService.isTodaySunday();
    return ResponseEntity.ok(Map.of("isSunday", isSunday));
  }

  @Operation(summary = "이번주 토론 영화(= 지난주 1위 영화) id 조회")
  @GetMapping("/this-week-movie")
  public ResponseEntity<Map<String, Long>> getThisWeekMovie() {
    Long tmdbId = voteService.getLastWeekTopVoteMovie();
    return ResponseEntity.ok(Map.of("tmdbId", tmdbId));
  }
}
