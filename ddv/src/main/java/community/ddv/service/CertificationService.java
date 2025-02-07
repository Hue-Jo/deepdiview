package community.ddv.service;

import community.ddv.constant.CertificationStatus;
import community.ddv.constant.ErrorCode;
import community.ddv.constant.RejectionReason;
import community.ddv.dto.CertificationDTO.CertificationResponseDto;
import community.ddv.entity.Certification;
import community.ddv.entity.User;
import community.ddv.exception.DeepdiviewException;
import community.ddv.repository.CertificationRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificationService {

  private final CertificationRepository certificationRepository;
  private final FileStorageService fileStorageService;
  private final UserService userService;

  /**
   * 일반사용자 _ 영화를 봤는지 인증을 위한 인증샷 제출 (특정 영화의 리뷰에 참여하기 위함)
   * @param file
   */
  @Transactional
  public CertificationResponseDto submitCertification(MultipartFile file) throws IOException {

    log.info("인증샷 업로드 시도");
    User user = userService.getLoginUser();

    // 이미 인증 승인을 받은 경우, 중복 인증 불가
    if (certificationRepository.existsByUser_IdAndStatus(user.getId(), CertificationStatus.APPROVED)) {
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    String certificationUrl = fileStorageService.uploadFile(file);
    log.info("S3에 인증샷 업로드 완료 : {} ", certificationUrl);

    Certification certification = Certification.builder()
        .user(user)
        .certificationUrl(certificationUrl)
        .status(CertificationStatus.PENDING) // 기본값 : 보류
        .createdAt(LocalDateTime.now())
        .build();

    Certification savedCertification = certificationRepository.save(certification);
    log.info("인증샷 업로드 성공");
    return CertificationResponseDto.builder()
        .id(savedCertification.getId())
        .userId(savedCertification.getUser().getId())
        .certificationUrl(savedCertification.getCertificationUrl())
        .status(savedCertification.getStatus())
        .createdAt(savedCertification.getCreatedAt())
        .build();
  }

//  /**
//   * 관리자 _ 인증 대기 목록 조회
//   * @param pageable
//   * @return
//   */
//  public Page<CertificationResponseDto> getPendingCertifications(Pageable pageable) {
//    userService.getLoginUser();
//    return certificationRepository.findByStatus(CertificationStatus.PENDING, pageable)
//        .map(certification -> CertificationResponseDto.builder()
//            .id(certification.getId())
//            .userId(certification.getUser().getId())
//            .certificationUrl(certification.getCertificationUrl())
//            .createdAt(certification.getCreatedAt())
//            .build());
//  }

  /**
   * 관리자 _ 인증 목록 조회 (인증 상태에 따른 필터링 가능)
   * @param status (인증상태 PENDING, APPROVED, REJECTED)
   * @param pageable
   */
  public Page<CertificationResponseDto> getCertificationsByStatus(CertificationStatus status, Pageable pageable) {
    userService.getLoginUser();

    if (status == null) {
      // 전체 조회
      return certificationRepository.findAll(pageable)
          .map(certification -> CertificationResponseDto.builder()
              .id(certification.getId())
              .userId(certification.getUser().getId())
              .certificationUrl(certification.getCertificationUrl())
              .status(certification.getStatus())
              .rejectionReason(certification.getRejectionReason())
              .createdAt(certification.getCreatedAt())
              .build());
    } else {
      // 인증 상태에 따른 조회
      return certificationRepository.findByStatus(status, pageable)
          .map(certification -> CertificationResponseDto.builder()
              .id(certification.getId())
              .userId(certification.getUser().getId())
              .certificationUrl(certification.getCertificationUrl())
              .status(certification.getStatus())
              .rejectionReason(certification.getRejectionReason())
              .createdAt(certification.getCreatedAt())
              .build());
    }

  }

  /**
   * 관리자 _ 특정 인증 정보 가져오는 메서드 (이미지 확인용)
   * @param certificationId
   * @return
   */
  public CertificationResponseDto getCertification(Long certificationId) {
    userService.getLoginUser();
    Certification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    return CertificationResponseDto.builder()
        .id(certificationId)
        .userId(certification.getUser().getId())
        .certificationUrl(certification.getCertificationUrl())
        .createdAt(certification.getCreatedAt())
        .status(certification.getStatus())
        .rejectionReason(certification.getRejectionReason())
        .build();
  }

  /**
   * 관리자 _ 인증 처리 (승인 : true, 거절 : false) & 거절 메시지
   * @param certificationId
   * @param approve
   * @param rejectionReason
   */
  public void proceedCertification(Long certificationId, boolean approve, RejectionReason rejectionReason) {
    userService.getLoginUser();

    Certification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    log.info("인증 상태 : {}", certification.getStatus());
    certification.setStatus(approve ? CertificationStatus.APPROVED : CertificationStatus.REJECTED,
                            approve ? RejectionReason.NONE : rejectionReason);
    log.info("인증 상태 변경 : {}", certification.getStatus());
    certificationRepository.save(certification);
  }


  /**
   * 특정 사용자가 인증 승인을 받았는지 확인
   */
  public boolean isUserCertificated(Long userId) {
    return certificationRepository.existsByUser_IdAndStatus(userId, CertificationStatus.APPROVED);
  }

}
