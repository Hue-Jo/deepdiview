package community.ddv.controller;

import community.ddv.dto.VoteDTO.VoteRequestDto;
import community.ddv.dto.VoteDTO.VoteResponseDTO;
import community.ddv.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<VoteResponseDTO> createVote(@RequestBody VoteRequestDto voteRequestDto) {
    VoteResponseDTO responseDTO = voteService.createVote(voteRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
  }
}
