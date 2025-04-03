package community.ddv.domain.certification;

import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationDTO {

    @Getter
    public static class CertificationRequestDto {

        private boolean approve;
        private RejectionReason rejectionReason;
    }

    @Getter
    @Builder
    public static class CertificationResponseDto {

        private Long id;
        private Long userId;
        private String certificationUrl;
        private CertificationStatus status;
        private LocalDateTime createdAt;
        private RejectionReason rejectionReason;
    }



}
