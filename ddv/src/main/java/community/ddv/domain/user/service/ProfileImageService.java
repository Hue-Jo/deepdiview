package community.ddv.domain.user.service;

import community.ddv.domain.user.entity.User;
import community.ddv.domain.user.repository.UserRepository;
import community.ddv.global.fileUpload.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService {

  private final UserService userService;
  private final FileStorageService fileStorageService;
  private final UserRepository userRepository;

  /**
   * 프로필 등록
   */
  @Transactional
  public String uploadProfileImage(MultipartFile profileImage) {
    User user = userService.getLoginUser();
    log.info("프로필사진 등록 요청");

    String profileImageUrl = fileStorageService.uploadFile(profileImage);
    log.info("S3에 프로필 이미지 업로드 완료");

    user.updateProfileImageUrl(profileImageUrl);
    log.info("프로필 이미지 등록 완료");
    return profileImageUrl;
  }

  /**
   * 프로필 수정
   * @param profileImage
   * */
  @Transactional
  public String updateProfileImage(MultipartFile profileImage) {
    User user = userService.getLoginUser();
    log.info("프로필사진 수정 요청");

    // 기존 프로필 삭제
    if (user.getProfileImageUrl() != null) {
      fileStorageService.deleteFile(user.getProfileImageUrl());
      log.info("기존 프로필 사진 삭제 완료");
    }

    String newProfileImageUrl = fileStorageService.uploadFile(profileImage);
    user.updateProfileImageUrl(newProfileImageUrl);
    log.info("프로필 이미지 수정 완료");

    return newProfileImageUrl;
  }

  /**
   * 프로필 사진 삭제
   */
  @Transactional
  public void deleteProfileImage() {
    User user = userService.getLoginUser();
    log.info("프로필사진 삭제 요청");

    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {

      fileStorageService.deleteFile(user.getProfileImageUrl());

      user.updateProfileImageUrl(null);
      log.info("프로필 사진 삭제 완료");
    }
  }
}
