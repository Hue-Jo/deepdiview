package community.ddv.domain.certification;

import community.ddv.domain.certification.CertificationDTO.CertificationDetailResponseDto;
import community.ddv.domain.certification.CertificationDTO.UserInformationDto;
import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import community.ddv.domain.certification.CertificationDTO.CertificationWrapperDto;
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
  public CertificationWrapperDto submitCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();
    log.info("[CERTIFICATION] 인증샷 제출 시도 - userId = {}", user.getId());

    if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("[CERTIFICATION] 일요일 인증샷 제출 불가");
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_ALLOWED_ON_SUNDAY);
    }

    // 이미 인증 승인을 받은 경우, 중복 인증 불가
    if (certificationRepository.existsByUser_IdAndStatus(user.getId(), CertificationStatus.APPROVED)) {
      log.warn("[CERTIFICATION] 이미 승인된 사용자 - userId = {}", user.getId());
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    String certificationUrl = fileStorageService.uploadFile(certificationImageFile);
    log.info("[CERTIFICATION] S3에 인증샷 업로드 완료");

    Certification certification = Certification.builder()
        .user(user)
        .certificationUrl(certificationUrl)
        .status(CertificationStatus.PENDING) // 기본값 : 보류
        .createdAt(LocalDateTime.now())
        .build();

    Certification savedCertification = certificationRepository.save(certification);
    log.info("[CERTIFICATION] 인증샷 업로드 완료 : certificationId = {}", savedCertification.getId());

    CertificationDetailResponseDto infoDto= convertToCertificationDto(savedCertification);
    return CertificationWrapperDto .builder()
        .status(savedCertification.getStatus())
        .certificationDetails(infoDto)
        .build();
  }

  /**
   * 자신이 올린 사진 URL, 상태 반환
   */
  @Transactional(readOnly = true)
  public CertificationWrapperDto getMyCertification() {

    validateSunday(); // 일요일에는 확인 불가
    User user = userService.getLoginUser();  // 로그인된 사용자 정보 가져오기

    LocalDate startOfWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    // 사용자가 제출한 인증샷 조회
    Certification certification = certificationRepository.findTopByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(
            user.getId(),
            startOfWeek.atStartOfDay(), // 월요일 0시 0분 0초
            endOfWeek.atTime(LocalTime.MAX)) // 토요일 11시 59분 59초
        .orElse(null);

    return certificationResponse(certification, user.getId());
  }

  /**
   * 일반사용자 _ 인증샷 수정
   */
  @Transactional
  public CertificationWrapperDto updateCertification(MultipartFile certificationImageFile) {

    User user = userService.getLoginUser();
    log.info("[CERTIFICATION] 인증샷 수정 시도 - userId = {}", user.getId());

    if (LocalDateTime.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      log.warn("[CERTIFICATION] 일요일에는 인증샷 수정이 불가");
      throw new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_ALLOWED_ON_SUNDAY);
    }

    // 이미 인증 승인을 받은 경우, 수정 불가
    Certification certification = certificationRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    if (certification.getStatus() == CertificationStatus.APPROVED) {
      log.warn("[CERTIFICATION] 이미 승인 받은 사용자는 인증샷 수정 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    // 기존의 인증샷 S3에서 삭제
    fileStorageService.deleteFile(certification.getCertificationUrl());

    // 새 인증샷 파일 업로드
    String newCertificationUrl = fileStorageService.uploadFile(certificationImageFile);

    // 인증샷 수정
    certification.updateCertification(newCertificationUrl);

    Certification updatedCertification = certificationRepository.save(certification);
    log.info("[CERTIFICATION] 인증샷 수정 완료 : certificationId = {}", updatedCertification.getId());

    return certificationResponse(updatedCertification, user.getId());
  }

  /**
   * 일반사용자 _ 인증샷 삭제
   */
  @Transactional
  public void deleteCertification() {

    User user = userService.getLoginUser();
    log.info("[CERTIFICATION] 인증샷 삭제 시도 - userId = {}", user.getId());

    // 인증샷 조회
    Certification certification = certificationRepository.findTopByUser_IdOrderByCreatedAtDesc(user.getId())
        .orElseThrow(() -> new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND));

    // 이미 승인 받은 상태면 삭제 불가
    if (certification.getStatus() == CertificationStatus.APPROVED) {
      log.warn("[CERTIFICATION] 승인된 사용자는 인증샷 삭제 불가");
      throw new DeepdiviewException(ErrorCode.ALREADY_APPROVED);
    }

    // 인증 상태를 NONE으로 초기화
    certification.resetStatus();

    // S3에서 파일 삭제
    fileStorageService.deleteFile(certification.getCertificationUrl());

    // DB에서 인증샷 삭제
    certificationRepository.delete(certification);
    log.info("[CERTIFICATION] 인증샷 삭제 완료 : certificationId = {}", certification.getId());
  }

  /**
   * 관리자 _ 인증 목록 조회 (인증 상태에 따른 필터링 가능)
   * @param status   (인증상태 PENDING, APPROVED, REJECTED)
   */
  @Transactional(readOnly = true)
  public CursorPageResponse<CertificationWrapperDto> getCertificationsByStatus(
      CertificationStatus status, LocalDateTime cursorCreatedAt, Long cursorId, int size) {

    validateSunday(); // 일요일에는 확인 불가

    User admin = userService.getLoginUser();
    log.info("[CERTIFICATION] 관리자의 인증 목록 조회 시작 - userId = {}", admin.getId());

    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("[CERTIFICATION] 관리자만 처리 가능합니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    LocalDateTime startOfWeek = getStartOfThisWeek();
    LocalDateTime endOfWeek = getEndOfThisWeek();

    // 처음 목록 요청시, 커서정보가 없기 때문에 가장 오래된 시간, 0L로 기본값으로 주기
    if (cursorCreatedAt == null || cursorCreatedAt.isBefore(startOfWeek)) {
      cursorCreatedAt = startOfWeek;
    }
    if (cursorId == null) {
      cursorId = 0L;
    }

    // cursor 에서는 페이지 개념이 없으나 몇 개를 가져올지는 정해야 함
    Pageable pageable = PageRequest.of(0, size);

    List<Certification> certifications;
    if (status == null) {
      // 전체 조회
      log.info("[CERTIFICATION] 인증 전체 조회");
      certifications = certificationRepository.findAllWithCursor(cursorCreatedAt, cursorId, endOfWeek, pageable);
    } else {
      // 인증 상태에 따른 조회
      log.info("[CERTIFICATION] 인증 상태에 따른 조회 : status = {}", status);
      certifications = certificationRepository.findByStatusWithCursor(status,cursorCreatedAt, cursorId, endOfWeek, pageable);
    }

    // 다음 페이지 존재 여부 판단
    boolean hasNext = certifications.size() == size;

    // 다음 요청을 위한 커서 값 준비
    LocalDateTime nextCreatedAt = null;
    Long nextCertificationId = null;

    if (hasNext && !certifications.isEmpty()) {
      // 리스트의 맨 마지막 데이터를 기억해서 다음 요청에 이용하기
      Certification lastCertification = certifications.get(certifications.size() - 1);
      nextCreatedAt = lastCertification.getCreatedAt();
      nextCertificationId = lastCertification.getId();
    }

    List<CertificationWrapperDto> certificationWrapperDtos = certifications.stream()
        .map(certification -> certificationResponse(certification, null))
        .toList();

    return new CursorPageResponse<>(certificationWrapperDtos, nextCreatedAt, nextCertificationId, hasNext);

  }


  /**
   * 관리자 _ 인증 처리 (승인 : true, 거절 : false) & 거절 메시지
   * @param certificationId
   * @param approve
   * @param rejectionReason
   */
  @Transactional
  public CertificationWrapperDto proceedCertification(Long certificationId, boolean approve, RejectionReason rejectionReason) {
    log.info("[CERTIFICATION] 인증 처리 시작 : certificationId = {}", certificationId);
    User admin = userService.getLoginUser();
    if (!admin.getRole().equals(Role.ADMIN)) {
      log.error("[CERTIFICATION] 관리자만 처리 가능합니다.");
      throw new DeepdiviewException(ErrorCode.ONLY_ADMIN_CAN);
    }

    Certification certification = certificationRepository.findById(certificationId)
        .orElseThrow(() -> {
          log.error("[CERTIFICATION] 인증 정보 없음 - certificationId = {}", certificationId);
          return new DeepdiviewException(ErrorCode.CERTIFICATION_NOT_FOUND);
        });

    if (approve) {
      if (rejectionReason != null) {
        log.warn("[CERTIFICATION] 승인 요청에 거절 사유 포함 : certificationId = {}", certificationId);
        throw new DeepdiviewException(ErrorCode.APPROVAL_SHOULD_NOT_HAVE_REASON);
      }
      certification.approve();
      log.info("[CERTIFICATION] 인증 승인 완료 - certificationId = {}", certificationId);
    } else {
      certification.reject(rejectionReason);
      log.info("[CERTIFICATION] 인증 거절 완료 : certificationId = {}, 거절 사유 = {}", certificationId, rejectionReason);
    }
    log.info("[CERTIFICATION] 인증 상태 변경 완료 : certificationId = {}, newStatus = {} ", certificationId, certification.getStatus());

    certificationRepository.save(certification);

    notificationService.certificateResult(certification.getId(), certification.getStatus());

    return certificationResponse(certification, null);
  }


  private CertificationWrapperDto certificationResponse(Certification certification, Long userId) {

    if (certification == null || certification.getStatus() == null) {
      return CertificationWrapperDto.builder()
          .status(CertificationStatus.NONE)
          .certificationDetails(null)
          .userInformation(null)
          .build();
    }

    User user = certification.getUser();
    boolean isOwner = user.getId().equals(userId);

    return CertificationWrapperDto.builder()
        .status(certification.getStatus())
        .certificationDetails(convertToCertificationDto(certification))
        .userInformation(isOwner ? null : convertToUserInformationDto(certification.getUser()))
        .build();
  }

  private CertificationDetailResponseDto convertToCertificationDto(Certification certification) {
    return CertificationDetailResponseDto.builder()
        .id(certification.getId())
        .certificationUrl(certification.getCertificationUrl())
        .createdAt(certification.getCreatedAt())
        .rejectionReason(certification.getRejectionReason())
        .build();
  }

  private UserInformationDto convertToUserInformationDto(User user) {
    return UserInformationDto.builder()
        .userId(user.getId())
        .userNickname(user.getNickname())
        .profileImageUrl(user.getProfileImageUrl())
        .build();
  }

  // 인증상태 초기화 (새로운 주가 시작되기 전, 인증 상태를 null (API로는 NONE)로 초기화)
  @Transactional
  public void resetCertificationStatus() {

    // 이번주 월-토에 들어온 인증들
    LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
    LocalDateTime endOfWeek = LocalDate.now().with(DayOfWeek.SATURDAY).atTime(LocalTime.MAX);
    // 테스트용 endOfWeek
    //localDateTime endOfWeek = LocalDateTime.now();

    List<Certification> certifications = certificationRepository.findAllByCreatedAtBetween(startOfWeek, endOfWeek);
    if (certifications.isEmpty()) {
      log.warn("[CERTIFICATION] 이번 주 인증 데이터가 없어 초기화를 스킵합니다.");
      return;
    }

    // S3에서 파일 삭제
    for (Certification certification : certifications) {
      try {
        fileStorageService.deleteFile(certification.getCertificationUrl());
      } catch (RuntimeException e) {
        log.info("[CERTIFICATION] 인증샷 파일을 S3에서 삭제 실패, url = {}", certification.getCertificationUrl());
      }
    }
    certificationRepository.resetAllCertifications();
    log.info("[CERTIFICATION] 인증 상태 초기화 완료 - 초기화 개수 = {}", certifications.size());
  }

  // 사용자가 특정 영화에 대해 인증된 상태인지 확인
  public boolean isUserCertified(Long userId) {
    return certificationRepository.existsByUser_IdAndStatus(userId, CertificationStatus.APPROVED);
  }

  // 일요일에는 확인 불가 예외처리 메서드
  private void validateSunday() {
    if (LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY) {
      throw new DeepdiviewException(ErrorCode. CERTIFICATION_CHECK_NOT_ALLOWED_ON_SUNDAY);
    }
  }

  // 월요일 0시
  private LocalDateTime getStartOfThisWeek() {
    return LocalDate.now()
        .with(DayOfWeek.MONDAY)
        .atStartOfDay();
  }

  // 토요일 23.59.59
  private LocalDateTime getEndOfThisWeek() {
    return LocalDate.now()
        .with(DayOfWeek.SATURDAY)
        .atTime(LocalTime.MAX);
  }
}
