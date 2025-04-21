package community.ddv.global.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursorPageResponse<T> {

  private List<T> content; // 데이터들
  private LocalDateTime nextCreatedAt; // 마지막 인증의 생성시간
  private Long nextCertificationId; // 마지막 인증의 ID
  private boolean hasNext; // 더 많은 데이터가 있는지 여부

}
