package community.ddv.controller;

import community.ddv.constant.CertificationStatus;
import community.ddv.dto.CertificationDTO.CertificationRequestDto;
import community.ddv.dto.CertificationDTO.CertificationResponseDto;
import community.ddv.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
@Tag(name = "Certification", description = "인증관련 API에 대한 명세를 제공합니다.")
public class CertificationController {

  private final CertificationService certificationService;

  @Operation(summary = "인증샷 제출", description = "파일 업로드")
  @PostMapping
  public ResponseEntity<CertificationResponseDto> submitCertification(
      @RequestParam("file") MultipartFile file) throws Exception {
    return ResponseEntity.ok(certificationService.submitCertification(file));
  }

  @Operation(summary = "인증샷, 상태 확인", description = "인증샷 url, 인증상태 반환")
  @GetMapping("/me")
  public ResponseEntity<CertificationResponseDto> getCertification() {
    CertificationResponseDto certificationResponseDto = certificationService.getMyCertification();
    return ResponseEntity.ok(certificationResponseDto);
  }

  @Operation(summary = "인증샷 수정", description = "파일 재업로드, PENDING/REJECTED 상태의 유저만 사용 가능")
  @PutMapping
  public ResponseEntity<CertificationResponseDto> updateCertification(
      @RequestParam("file") MultipartFile file) throws Exception {
    return ResponseEntity.ok(certificationService.updateCertification(file));
  }

  @Operation(summary = "인증샷 삭제", description = "PENDING/REJECTED 상태의 유저만 사용 가능")
  @DeleteMapping
  public ResponseEntity<Void> deleteCertification() throws IOException {
    certificationService.deleteCertification();
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "인증 목록 조회", description = "관리자 전용 - 보류, 승인, 거절 필터링 가능 | 한 페이지당 10개씩 반환 ㅣ 인증요청을 한 지 오래된 순서대로 정렬됩니다.")
  @GetMapping("/admin")
  public ResponseEntity<Page<CertificationResponseDto>> getPendingCertifications(
      @RequestParam(required = false) CertificationStatus status,
      @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Direction.ASC) Pageable pageable) {
    Page<CertificationResponseDto> certifications = certificationService.getCertificationsByStatus(
        status, pageable);
    return ResponseEntity.status(HttpStatus.OK).body(certifications);
  }


  @Operation(summary = "인증 승인/거절", description = "관리자 전용 - 승인 : true / 거절 : false | 거절시 rejectionReason: OTHER_MOVIE_IMAGE, WRONG_IMAGE, UNIDENTIFIABLE_IMAGE")
  @PostMapping("/admin/proceeding/{certificationId}")
  public ResponseEntity<Void> proceedCertification(
      @PathVariable Long certificationId,
      @RequestBody CertificationRequestDto requestDto) {
    certificationService.proceedCertification(certificationId, requestDto.isApprove(), requestDto.getRejectionReason());
    return ResponseEntity.noContent().build();
  }
}


