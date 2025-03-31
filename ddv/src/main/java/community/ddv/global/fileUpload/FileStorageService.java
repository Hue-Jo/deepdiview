package community.ddv.global.fileUpload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import community.ddv.global.constant.ErrorCode;
import community.ddv.global.exception.DeepdiviewException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

  private final AmazonS3 amazonS3;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${cloud.aws.region.static}")
  private String region;

  // 허용된 확장자 리스트
  private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

  /**
   * S3에 이미지 업로드
   * @param file
   */
  public String uploadFile(MultipartFile file) throws IOException {

    if (file.isEmpty()) {
      throw new IOException("파일이 존재하지 않습니다.");
    }

    String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
    if (fileExtension == null || !ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
      throw new DeepdiviewException(ErrorCode.IMAGE_FILE_ONLY);
    }

    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename(); // 고유한 파일 이름 생성

    // 메타데이터 설정
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    // S3에 파일 업로드 요청 생성
    PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata);

    try {
      // S3에 파일 업로드
      amazonS3.putObject(putObjectRequest);
    } catch (AmazonServiceException e) {
      log.error("Amazon Service Exception: {}", e.getMessage());
      throw new IOException("S3에 파일 업로드 중 문제가 발생했습니다.");
    }

    return getImageUrl(fileName);
  }

  private String getImageUrl(String fileName) {
    return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
  }

  public void deleteFile(String fileUrl) throws IOException {
    if (fileUrl != null && !fileUrl.isEmpty()) {
      String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
      try {
        amazonS3.deleteObject(bucket, fileName);
        log.info("S3에서 파일 삭제 성공");
      } catch (AmazonServiceException e) {
        log.error("S3에서 파일 삭제 중 오류 발생");
        throw new RuntimeException("파일 삭제 중 문제가 발생했습니다. 관리자에게 문의하세요");
      }
    }
  }
}
