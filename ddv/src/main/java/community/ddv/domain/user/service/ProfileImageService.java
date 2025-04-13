package community.ddv.domain.user.service;

import community.ddv.domain.user.entity.User;
import community.ddv.global.fileUpload.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService {

  @Value("${profile.image.default-url}")
  private String defaultProfileImageUrl;

  private final UserService userService;
  private final FileStorageService fileStorageService;


  @Transactional
  public String updateProfileImage(MultipartFile profileImage) {

    User user = userService.getLoginUser();
    String existingProfileImageUrl = user.getProfileImageUrl();

    // 기존 프사가 존재하는 경우, 삭제 후 새 이미지로 대체 (디폴트 프사가 아닐 때!)
    if (existingProfileImageUrl != null && !existingProfileImageUrl.isEmpty()
    && !isDefaultProfileImage(existingProfileImageUrl)) {
      fileStorageService.deleteFile(existingProfileImageUrl);
    }

    String newProfileImageUrl = fileStorageService.uploadFile(profileImage);
    user.updateProfileImageUrl(newProfileImageUrl);
    log.info("프로필 이미지 등록/수정 완료");
    return newProfileImageUrl;

  }

  private boolean isDefaultProfileImage(String imageUrl) {
    return imageUrl.contains(defaultProfileImageUrl);
  }

  /**
   * 프로필 사진 삭제
   */
  @Transactional
  public String deleteProfileImage() {
    User user = userService.getLoginUser();
    log.info("프로필사진 삭제 요청");

    String profileImageUrl = user.getProfileImageUrl();
    if (profileImageUrl != null && !profileImageUrl.isEmpty()
        && !profileImageUrl.equals(defaultProfileImageUrl)) {

      fileStorageService.deleteFile(user.getProfileImageUrl());
    }
    user.updateProfileImageUrl(defaultProfileImageUrl);
    log.info("기본 프로필로 초기화");
    return defaultProfileImageUrl;
  }
}