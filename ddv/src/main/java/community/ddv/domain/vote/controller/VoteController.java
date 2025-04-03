package community.ddv.domain.vote.controller;

import community.ddv.domain.vote.dto.VoteDTO.VoteCreatedDTO;
import community.ddv.domain.vote.dto.VoteDTO.VoteResultDTO;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteOptionsDto;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteParticipationRequestDto;
import community.ddv.domain.vote.dto.VoteParticipationDTO.VoteParticipationResponseDto;
import community.ddv.domain.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Tag(name = "Vote", description = "투표 관련 API에 대한 명세를 제공합니다.")
public class VoteController {

  private final VoteService voteService;

  @Operation(summary = "투표 생성", description = "관리자 전용, 일요일만 투표 생성 가능")
  @PostMapping
  public ResponseEntity<VoteCreatedDTO> createVote() {
    VoteCreatedDTO responseDTO = voteService.createVote();
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
  }

  @Operation(summary = "현재 진행중인 투표의 선택지 조회", description = "현재 투표가 진행중일 때만 조회 가능합니다.")
  @GetMapping("/options")
  public ResponseEntity<VoteOptionsDto> getActivatingVote() {
    VoteOptionsDto options = voteService.getVoteChoices();
    return ResponseEntity.status(HttpStatus.OK).body(options);
  }

  @Operation(summary = "현재 진행중인 투표에 참여하기")
  @PostMapping("/participate")
  public ResponseEntity<VoteParticipationResponseDto> participateVote(
      @Valid @RequestBody VoteParticipationRequestDto voteParticipationRequestDto) {
    VoteParticipationResponseDto responseDTO = voteService.participateVote(voteParticipationRequestDto);
    return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
  }

  @Operation(summary = "현재 진행중인 투표 결과 확인", description = "현재 투표가 진행중일 때만 결과 확인 가능합니다.")
  @GetMapping("/result")
  public ResponseEntity<VoteResultDTO> getVoteResult() {
    VoteResultDTO voteResultDTO = voteService.getCurrentVoteResult();
    return ResponseEntity.ok(voteResultDTO);
  }

  @Operation(summary = "투표 삭제", description = "관리자만 삭제 가능")
  @DeleteMapping("/{voteId}")
  public ResponseEntity<Void> deleteVote(
      @PathVariable Long voteId) {
    voteService.deleteVote(voteId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "현재 진행중인 투표에 참여했는지 여부 T/F")
  @GetMapping("/participation-status")
  public ResponseEntity<Map<String, Boolean>> checkParticipationStatus() {
    boolean participated = voteService.isUserAlreadyParticipatedInCurrentVote();
    return ResponseEntity.ok(Map.of("participated", participated));
  }

  @Operation(summary = "지난주 투표 전체 결과 조회", description = "지난주에 진행한 투표의 전체 결과를 볼 수 있습니다.")
  @GetMapping("/result/latest")
  public ResponseEntity<VoteResultDTO> getLatestVoteResult() {
    VoteResultDTO voteResultDTO = voteService.getLatestVoteResult();
    return ResponseEntity.ok(voteResultDTO);
  }
}
