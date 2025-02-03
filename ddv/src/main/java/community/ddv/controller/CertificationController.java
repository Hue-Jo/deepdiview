package community.ddv.controller;

import community.ddv.constant.RejectionReason;
import community.ddv.dto.CertificationDTO;
import community.ddv.dto.CertificationDTO.CertificationRequestDto;
import community.ddv.dto.CertificationDTO.CertificationResponseDto;
import community.ddv.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

  private final CertificationService certificationService;

  @Operation(summary = "인증샷 제출", description = "파일 업로드")
  @PostMapping
  public ResponseEntity<CertificationResponseDto> submitCertification(
      @RequestParam("file") MultipartFile file) throws Exception {
    return ResponseEntity.ok(certificationService.submitCertification(file));
  }

  @Operation(summary = "인증 대기자 목록 조회", description = "관리자 전용")
  @GetMapping("/admin/pending")
  public ResponseEntity<Page<CertificationResponseDto>> getPendingCertifications(Pageable pageable) {
    Page<CertificationResponseDto> certifications = certificationService.getPendingCertifications(
        pageable);
    return ResponseEntity.status(HttpStatus.OK).body(certifications);
  }

  @Operation(summary = "특정 인증정보 조회_인증샷 확인", description = "관리자 전용")
  @GetMapping("/admin/{certificationId}")
  public ResponseEntity<CertificationResponseDto> getCertificationById(
      @PathVariable Long certificationId) {
    CertificationResponseDto certification = certificationService.getCertification(certificationId);
    return ResponseEntity.status(HttpStatus.OK).body(certification);
  }

  @Operation(summary = "인증 승인/거절", description = "관리자 전용. 승인 : true / 거절 : false")
  @PostMapping("/admin/proceeding/{certificationId}")
  public ResponseEntity<Void> proceedCertification(
      @PathVariable Long certificationId,
      @RequestBody CertificationRequestDto requestDto) {
    certificationService.proceedCertification(certificationId, requestDto.isApprove(), requestDto.getRejectionReason());
    return ResponseEntity.noContent().build();
  }
}


