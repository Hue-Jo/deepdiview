package community.ddv.global.fileUpload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import community.ddv.global.exception.ErrorCode;
import community.ddv.global.exception.DeepdiviewException;
import java.io.IOException;
import java.io.InputStream;
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

  // 파일 크기 제한 (5MB)
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

  // 허용된 확장자 리스트
  private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

  // 파일이 비어있는지 확인하고, 크기 제한을 체크하는 메서드
  public void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new DeepdiviewException(ErrorCode.FILE_NOT_FOUND); // 파일이 비어 있을 때
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new DeepdiviewException(ErrorCode.FILE_SIZE_EXCEEDED); // 파일 크기 초과 시
    }

    // 파일 확장자 체크
    String fileExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
    if (fileExtension == null || !ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
      throw new DeepdiviewException(ErrorCode.IMAGE_FILE_ONLY); // 유효하지 않은 파일 확장자
    }
  }

  /**
   * S3에 이미지 업로드
   * @param file
   */
  public String uploadFile(MultipartFile file) {

    validateFile(file);

    String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename(); // 고유한 파일 이름 생성

    // 메타데이터 설정
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(file.getContentType());
    metadata.setContentLength(file.getSize());

    try (InputStream inputStream = file.getInputStream()) {
      PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, inputStream, metadata);
      amazonS3.putObject(putObjectRequest);
      log.info("[FILE] S3에 파일 업로드 성공");
    } catch (IOException e) {
      log.error("[FILE] 파일 업로드 중 IOException 발생: {}", e.getMessage());
      throw new DeepdiviewException(ErrorCode.FILE_UPLOAD_FAILED);
    } catch (AmazonServiceException e) {
      log.error("[FILE] Amazon Service Exception: {}", e.getMessage());
      throw new DeepdiviewException(ErrorCode.FILE_UPLOAD_FAILED);
    }

    return getImageUrl(fileName);
  }

  private String getImageUrl(String fileName) {
    return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, fileName);
  }

  public void deleteFile(String fileUrl) {
    if (fileUrl != null && !fileUrl.isEmpty()) {
      String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
      try {
        amazonS3.deleteObject(bucket, fileName);
        log.info("[FILE] S3에서 파일 삭제 성공");
      } catch (AmazonServiceException e) {
        log.error("[FILE] S3에서 파일 삭제 중 오류 발생");
        throw new DeepdiviewException(ErrorCode.FILE_DELETE_FAILED);
      }
    }
  }
}
