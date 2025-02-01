package community.ddv.dto;

import community.ddv.constant.CertificationStatus;
import community.ddv.constant.RejectionReason;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationDTO {

    private Long id;
    private Long userId;
    private String certificationUrl;
    private CertificationStatus status;
    private LocalDateTime createdAt;
    private RejectionReason rejectionReason;

}
