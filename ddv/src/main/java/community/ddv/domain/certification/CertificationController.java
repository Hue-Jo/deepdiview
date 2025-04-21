package community.ddv.domain.certification;

import community.ddv.domain.certification.CertificationDTO.CertificationRequestDto;
import community.ddv.domain.certification.CertificationDTO.CertificationResponseDto;
import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CertificationResponseDto> submitCertification(
      @Parameter(description = "인증샷 파일")
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(certificationService.submitCertification(file));
  }

  @Operation(summary = "인증샷, 상태 확인", description = "인증샷 url, 인증상태 반환")
  @GetMapping("/me")
  public ResponseEntity<CertificationResponseDto> getCertification() {
    CertificationResponseDto certificationResponseDto = certificationService.getMyCertification();
    return ResponseEntity.ok(certificationResponseDto);
  }

  @Operation(summary = "인증샷 수정", description = "파일 재업로드, PENDING/REJECTED 상태의 유저만 사용 가능")
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CertificationResponseDto> updateCertification(
      @Parameter(description = "인증샷 파일")
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(certificationService.updateCertification(file));
  }

  @Operation(summary = "인증샷 삭제", description = "PENDING/REJECTED 상태의 유저만 사용 가능")
  @DeleteMapping
  public ResponseEntity<Void> deleteCertification() {
    certificationService.deleteCertification();
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "인증 목록 조회 (실시간성 보장을 위해 커서기반 페이징 사용)", description = "관리자 전용 - 보류, 승인, 거절 필터링 가능 | 한 번에 10개씩 반환 ㅣ 인증요청을 한 지 오래된 순서대로 정렬됩니다.")
  @GetMapping("/admin")
  public ResponseEntity<CursorPageResponse<CertificationResponseDto>> getPendingCertifications(
      @Parameter(description = "PENDING, APPROVED, REJECTED만 사용")
      @RequestParam(required = false) CertificationStatus status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
      @RequestParam(required = false) Long certificationId,
      @RequestParam(defaultValue = "10") int size)
  {
    CursorPageResponse<CertificationResponseDto> certifications =
        certificationService.getCertificationsByStatus(status, createdAt, certificationId, size);
    return ResponseEntity.ok(certifications);
  }


  @Operation(summary = "인증 승인/거절", description = "관리자 전용 - 승인 : true / 거절 : false | 거절시 rejectionReason: OTHER_MOVIE_IMAGE, WRONG_IMAGE, UNIDENTIFIABLE_IMAGE")
  @PostMapping("/admin/proceeding/{certificationId}")
  public ResponseEntity<CertificationResponseDto> proceedCertification(
      @PathVariable Long certificationId,
      @RequestBody CertificationRequestDto requestDto) {
    CertificationResponseDto responseDto = certificationService.proceedCertification(certificationId, requestDto.isApprove(), requestDto.getRejectionReason());
    return ResponseEntity.ok(responseDto);
  }
}


