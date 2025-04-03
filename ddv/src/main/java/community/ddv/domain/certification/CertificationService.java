package community.ddv.domain.certification;

import community.ddv.domain.certification.CertificationDTO.CertificationResponseDto;
import community.ddv.domain.notification.NotificationService;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.constant.CertificationStatus;
import community.ddv.global.constant.RejectionReason;
import community.ddv.global.constant.Role;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import community.ddv.global.fileUpload.FileStorageService;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
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
  private final NotificationService notificationService;

  /**
   * 일반사용자 _ 영화를 봤는지 인증을 위한 인증샷 제출 (특정 영화의 리뷰에 참여하기 위함)
   * @param certificationImageFile
   */
  @Transactional
  public CertificationResponseDto submitCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();
    log.info("인증샷 업로드 시도 : userId = {}", user.getId());

    if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("일요일에는 인증샷 제출 불가");
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_ALLOWED_ON_SUNDAY);
    }

    // 이미 인증 승인을 받은 경우, 중복 인증 불가
    if (certificationRepository.existsByUser_IdAndStatus(user.getId(), CertificationStatus.APPROVED)) {
      log.warn("이미 승인을 받은 사용자 : userId = {}", user.getId());
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    String certificationUrl = fileStorageService.uploadFile(certificationImageFile);
    log.info("S3에 인증샷 업로드 완료 : url = {} ", certificationUrl);

    Certification certification = Certification.builder()
        .user(user)
        .certificationUrl(certificationUrl)
        .status(CertificationStatus.PENDING) // 기본값 : 보류
        .createdAt(LocalDateTime.now())
        .build();

    Certification savedCertification = certificationRepository.save(certification);
    log.info("인증샷 업로드 성공 : certificationId = {}", savedCertification.getId());
    return CertificationResponseDto.builder()
        .id(savedCertification.getId())
        .userId(savedCertification.getUser().getId())
        .certificationUrl(savedCertification.getCertificationUrl())
        .status(savedCertification.getStatus())
        .createdAt(savedCertification.getCreatedAt())
        .build();
  }

  /**
   * PENDING/REJECT 상태의 사용자 _ 자신이 올린 사진 URL, 상태 반환
   */
  @Transactional(readOnly = true)
  public CertificationResponseDto getMyCertification() {
    User user = userService.getLoginUser();  // 로그인된 사용자 정보 가져오기
    log.info("인증샷 조회 시도 : userId = {}", user.getId());

    // 사용자가 제출한 인증샷 조회
    Certification certification = certificationRepository.findByUser_Id(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    // PENDING 또는 REJECTED 상태에서만 반환
    if (!certification.getStatus().equals(CertificationStatus.APPROVED)) {
      return convertToCertificationDto(certification);
    }
    log.error("승인된 사용자는 자신의 상태를 조회할 필요가 없습니다.");
    throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
  }

  /**
   * 일반사용자 _ 인증샷 수정
   */
  @Transactional
  public CertificationResponseDto updateCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();
    log.info("인증샷 수정 시도 : userId = {}", user.getId());

    if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("일요일에는 인증샷 수정이 불가");
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_ALLOWED_ON_SUNDAY);
    }

    // 이미 인증 승인을 받은 경우, 수정 불가
    Certification certification = certificationRepository.findByUser_Id(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    if (certification.getStatus() == CertificationStatus.APPROVED) {
      log.warn("이미 승인 받은 사용자는 인증샷 수정 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    // 기존의 인증샷 S3에서 삭제
    fileStorageService.deleteFile(certification.getCertificationUrl());
    log.info("기존의 인증샷 파일 S3에서 삭제 완료");

    // 새 인증샷 파일 업로드
    String newCertificationUrl = fileStorageService.uploadFile(certificationImageFile);
    log.info("새로운 인증샷 파일 S3에 업로드 완료");

    // 인증샷 수정
    certification.setCertificationUrl(newCertificationUrl);
    certification.setStatus(CertificationStatus.PENDING);
    certification.setCreatedAt(LocalDateTime.now());

    Certification updatedCertification = certificationRepository.save(certification);
    log.info("인증샷 수정 완료");

    return CertificationResponseDto.builder()
        .id(updatedCertification.getId())
        .userId(updatedCertification.getUser().getId())
        .certificationUrl(updatedCertification.getCertificationUrl())
        .status(updatedCertification.getStatus())
        .createdAt(updatedCertification.getCreatedAt())
        .build();
  }

  /**
   * 일반사용자 _ 인증샷 삭제
   */
  @Transactional
  public void deleteCertification() {

    User user = userService.getLoginUser();
    log.info("인증샷 삭제 시도 : userId = {}", user.getId());

    // 인증샷 조회
    Certification certification = certificationRepository.findByUser_Id(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    // 이미 승인 받은 상태면 삭제 불가
    if (certification.getStatus() == CertificationStatus.APPROVED) {
      log.warn("승인된 사용자는 인증샷 삭제 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    // 인증 상태를 null로 초기화
    certification.setStatus(null, null);

    // S3에서 파일 삭제
    fileStorageService.deleteFile(certification.getCertificationUrl());
    log.info("인증샷 파일 S3에서 삭제 완료");

    // DB에서 인증샷 삭제
    certificationRepository.delete(certification);
    log.info("인증샷 삭제 완료 : certificationId = {}", certification.getId());
  }

  /**
   * 관리자 _ 인증 목록 조회 (인증 상태에 따른 필터링 가능)
   * @param status (인증상태 PENDING, APPROVED, REJECTED)
   * @param pageable
   */
  public Page<CertificationResponseDto> getCertificationsByStatus(CertificationStatus status, Pageable pageable) {
    log.info("관리자의 인증 목록 조회 시작");
    User admin = userService.getLoginUser();
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 처리 가능합니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }
    if (status == null) {
      // 전체 조회
      log.info("인증 전체 조회");
      return certificationRepository.findByStatusIsNotNull(pageable)
          .map(this::convertToCertificationDto);
    } else {
      // 인증 상태에 따른 조회
      log.info("인증 상태에 따른 조회 : staus = {}", status);
      return certificationRepository.findByStatus(status, pageable)
          .map(this::convertToCertificationDto);
    }

  }


  /**
   * 관리자 _ 인증 처리 (승인 : true, 거절 : false) & 거절 메시지
   * @param certificationId
   * @param approve
   * @param rejectionReason
   */
  public void proceedCertification(Long certificationId, boolean approve, RejectionReason rejectionReason) {
    log.info("인증 처리 시작 : certificationId = {}", certificationId);
    User admin = userService.getLoginUser();
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 처리 가능합니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }
    Certification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> {
          log.error("인증 정보를 찾을 수 없음");
          return new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND);
        });
    log.info("현재 인증 상태 : status = {}", certification.getStatus());

    if (approve) {
      certification.setStatus(CertificationStatus.APPROVED, null);
      log.info("인증 승인 : certificationId = {}", certificationId);
    } else {
      certification.setStatus(CertificationStatus.REJECTED, rejectionReason);
      log.info("인증 거절 : certificationId = {}, 거절 사유 = {}", certificationId, rejectionReason);
    }
    log.info("인증 상태 변경 완료 : certificationId = {}, newStatus = {} ", certificationId, certification.getStatus());
    certificationRepository.save(certification);

    notificationService.certificateResult(certification.getId(), certification.getStatus());
  }

  // 사용자가 특정 영화에 대해 인증된 상태인지 확인
  public boolean isUserCertified(Long userId) {
    return certificationRepository.existsByUser_IdAndStatus(userId, CertificationStatus.APPROVED);
  }

  private CertificationResponseDto convertToCertificationDto(Certification certification) {
    return CertificationResponseDto.builder()
        .id(certification.getId())
        .userId(certification.getUser().getId())
        .certificationUrl(certification.getCertificationUrl())
        .createdAt(certification.getCreatedAt())
        .status(certification.getStatus())
        .rejectionReason(certification.getRejectionReason())
        .build();
  }

  // 인증상태 초기화 (새로운 주가 시작되기 전, 인증 상태를 null로 초기화)
  @Transactional
  @Scheduled(cron = "0 0 0 * * SUN")
  // 테스트를 위해 매일 0시 정각에 초기화
  //@Scheduled(cron = "0 0 0 * * *")
  protected void resetCertificationStatus() {

    log.info("새로운 주가 됨에 따라 인증상태 초기화");

    List<Certification> certifications = certificationRepository.findAll();

    if (certifications.isEmpty()) {
      log.warn("초기화할 인증 데이터가 없어 초기화를 스킵합니다.");
      return;
    }

    // S3에서 파일 삭제
    for(Certification certification : certifications) {
      try {
        fileStorageService.deleteFile(certification.getCertificationUrl());
        log.info("인증샷 파일 S3에서 삭제 완료");
      } catch (RuntimeException e) {
        log.info("인증샷 파일을 S3에서 삭제 실패 : url = {}", certification.getCertificationUrl());
      }
    }

    int resetCount = certificationRepository.resetAllCertifications();

    log.info("초기화된 인증 개수 : {} ", resetCount);

  }

}
