package community.ddv.domain.certification;

import community.ddv.domain.certification.CertificationDTO.CertificationResponseDto;
import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import community.ddv.domain.notification.NotificationService;
import community.ddv.domain.user.constant.Role;
import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.service.UserService;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import community.ddv.global.fileUpload.FileStorageService;
import community.ddv.global.response.CursorPageResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
  private final NotificationService notificationService;

  /**
   * 일반사용자 _ 영화를 봤는지 인증을 위한 인증샷 제출 (특정 영화의 리뷰에 참여하기 위함)
   * @param certificationImageFile
   */
  @Transactional
  public CertificationResponseDto submitCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();

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
   * 자신이 올린 사진 URL, 상태 반환
   */
  @Transactional(readOnly = true)
  public CertificationResponseDto getMyCertification() {
    User user = userService.getLoginUser();  // 로그인된 사용자 정보 가져오기

    LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    // 사용자가 제출한 인증샷 조회
    Certification certification = certificationRepository.findTopByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            user.getId(),
            startOfWeek.atStartOfDay(), // 월요일 0시 0분 0초
            endOfWeek.atTime(LocalTime.MAX)) // 토요일 11시 59분 59초
        .orElse(null);

    if (certification == null) {
      return CertificationResponseDto.builder()
          .id(null)
          .userId(null)
          .certificationUrl(null)
          .status(CertificationStatus.NONE)
          .createdAt(null)
          .rejectionReason(null)
          .build();
    }

    CertificationStatus status = certification.getStatus();

    if (status == null) {
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND);
    }

    return convertToCertificationDto(certification);
  }

  /**
   * 일반사용자 _ 인증샷 수정
   */
  @Transactional
  public CertificationResponseDto updateCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();

    if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("일요일에는 인증샷 수정이 불가");
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_ALLOWED_ON_SUNDAY);
    }

    // 이미 인증 승인을 받은 경우, 수정 불가
    Certification certification = certificationRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    if (certification.getStatus() == CertificationStatus.APPROVED) {
      log.warn("이미 승인 받은 사용자는 인증샷 수정 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    // 기존의 인증샷 S3에서 삭제
    fileStorageService.deleteFile(certification.getCertificationUrl());

    // 새 인증샷 파일 업로드
    String newCertificationUrl = fileStorageService.uploadFile(certificationImageFile);

    // 인증샷 수정
    certification.setCertificationUrl(newCertificationUrl);
    certification.setStatus(CertificationStatus.PENDING);
    certification.setRejectionReason(null);
    certification.setCreatedAt(LocalDateTime.now());

    Certification updatedCertification = certificationRepository.save(certification);
    log.info("인증샷 수정 완료 : certificationId = {}", updatedCertification.getId());

    return CertificationResponseDto.builder()
        .id(updatedCertification.getId())
        .userId(updatedCertification.getUser().getId())
        .certificationUrl(updatedCertification.getCertificationUrl())
        .status(updatedCertification.getStatus())
        .rejectionReason(null)
        .createdAt(updatedCertification.getCreatedAt())
        .build();
  }

  /**
   * 일반사용자 _ 인증샷 삭제
   */
  @Transactional
  public void deleteCertification() {

    User user = userService.getLoginUser();

    // 인증샷 조회
    Certification certification = certificationRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
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

    // DB에서 인증샷 삭제
    certificationRepository.delete(certification);
    log.info("인증샷 삭제 완료 : certificationId = {}", certification.getId());
  }

  /**
   * 관리자 _ 인증 목록 조회 (인증 상태에 따른 필터링 가능)
   * @param status   (인증상태 PENDING, APPROVED, REJECTED)
   */
  @Transactional(readOnly = true)
  public CursorPageResponse<CertificationResponseDto> getCertificationsByStatus(
      CertificationStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int size) {

    log.info("관리자의 인증 목록 조회 시작");
    User admin = userService.getLoginUser();
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("관리자만 처리 가능합니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    // 처음 목록 요청시, 커서정보가 없기 때문에 가장 오래된 시간, 0L로 기본값으로 주기
    if (cursorCreatedAt == null) {
      cursorCreatedAt = LocalDateTime.MIN;
    }
    if (cursorId == null) {
      cursorId = 0L;
    }

    // cursor 에서는 페이지 개념이 없으나 몇 개를 가져올지는 정해야 함
    Pageable pageable = PageRequest.of(0, size);

    List<Certification> certifications;
    if (status == null) {
      // 전체 조회
      log.info("인증 전체 조회");
      certifications = certificationRepository.findAllWithCursor(cursorCreatedAt, cursorId, pageable);
    } else {
      // 인증 상태에 따른 조회
      log.info("인증 상태에 따른 조회 : status = {}", status);
      certifications = certificationRepository.findByStatusWithCursor(status,cursorCreatedAt, cursorId, pageable);
    }

    // 다음 커서 계산
    boolean hasNext = certifications.size() == size;
    LocalDateTime nextCreatedAt = null;
    Long nextCertificationId = null;

    if (hasNext && !certifications.isEmpty()) {
      // 리스트의 맨 마지막 데이터를 기억해서 다음 요청에 이용하기
      Certification lastCertification = certifications.get(certifications.size() - 1);
      nextCreatedAt = lastCertification.getCreatedAt();
      nextCertificationId = lastCertification.getId();
    }

    List<CertificationResponseDto> certificationResponses = certifications.stream()
        .map(this::convertToCertificationDto)
        .collect(Collectors.toList());

    return new CursorPageResponse<>(certificationResponses, nextCreatedAt, nextCertificationId, hasNext);

  }


  /**
   * 관리자 _ 인증 처리 (승인 : true, 거절 : false) & 거절 메시지
   * @param certificationId
   * @param approve
   * @param rejectionReason
   */
  @Transactional
  public CertificationResponseDto proceedCertification(Long certificationId, boolean approve, RejectionReason rejectionReason) {
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

    return convertToCertificationDto(certification);
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
  public void resetCertificationStatus() {

    log.info("새로운 주가 됨에 따라 인증상태 초기화");

    List<Certification> certifications = certificationRepository.findAll();

    if (certifications.isEmpty()) {
      log.warn("초기화할 인증 데이터가 없어 초기화를 스킵합니다.");
      return;
    }

    // S3에서 파일 삭제
    for (Certification certification : certifications) {
      try {
        fileStorageService.deleteFile(certification.getCertificationUrl());
      } catch (RuntimeException e) {
        log.info("인증샷 파일을 S3에서 삭제 실패 : url = {}", certification.getCertificationUrl());
      }
    }

    int resetCount = certificationRepository.resetAllCertifications();

    log.info("초기화된 인증 개수 : {} ", resetCount);

  }

}
