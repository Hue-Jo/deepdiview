package community.ddv.controller;

import community.ddv.dto.VoteDTO.VoteCreatedDTO;
import community.ddv.dto.VoteDTO.VoteResultDTO;
import community.ddv.dto.VoteParticipationDTO.VoteOptionsDto;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationRequestDto;
import community.ddv.dto.VoteParticipationDTO.VoteParticipationResponseDto;
import community.ddv.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
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
public class VoteController {

  private final VoteService voteService;

  @Operation(summary = "투표 생성", description = "관리자 전용, 일/월요일만 투표 생성 가능")
  @PostMapping
  public ResponseEntity<VoteCreatedDTO> createVote() {
    VoteCreatedDTO responseDTO = voteService.createVote();
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
  }

  @Operation(summary = "현재 진행중인 투표의 선택지 조회")
  @GetMapping("/options")
  public ResponseEntity<VoteOptionsDto> getActivatingVote() {
    VoteOptionsDto options = voteService.getVoteChoices();
    return ResponseEntity.status(HttpStatus.OK).body(options);
  }

  @Operation(summary = "현재 진행중인 투표에 참여하기")
  @PostMapping("/participate")
  public ResponseEntity<VoteParticipationResponseDto> participateVote(
      @RequestBody VoteParticipationRequestDto voteParticipationRequestDto) {
    VoteParticipationResponseDto responseDTO = voteService.participateVote(voteParticipationRequestDto);
    return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
  }

  @Operation(summary = "현재 진행중인 투표 결과 확인", description = "현재 투표가 진행중일 때만 결과 확인 가능합니다.")
  @GetMapping("/result")
  public ResponseEntity<VoteResultDTO> getVoteResult() {
    VoteResultDTO voteResultDTO = voteService.getCurrentVoteResult();
    return ResponseEntity.ok(voteResultDTO);
  }

  @Operation(summary = "가장 최근 진행됐던 투표 결과 조회", description = "투표가 진행중일 때도, 끝났을 때도 결과 조회가 가능합니다.")
  @GetMapping("/result/latest")
  public ResponseEntity<VoteResultDTO> getLatestVoteResult() {
    VoteResultDTO voteResultDTO = voteService.getLatestVoteResult();
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
  public ResponseEntity<Boolean> checkParticipationStatus() {
    boolean participated = voteService.isUserAlreadyParticipatedInCurrentVote();
    return ResponseEntity.ok(participated);
  }
}
