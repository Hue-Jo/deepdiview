package community.ddv.domain.certification;

import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationDTO {

    @Getter
    public static class CertificationRequestDto {

        @NotNull(message = "승인 여부를 선택해주세요")
        private Boolean approve;
        private RejectionReason rejectionReason;
    }

    @Getter
    @Builder
    public static class CertificationWrapperDto {

        private CertificationStatus status;
        private CertificationDetailResponseDto certificationDetails;
        private UserInformationDto userInformation;
    }

    @Getter
    @Builder
    public static class CertificationDetailResponseDto {

        private Long id;
        private String certificationUrl;
        private LocalDateTime createdAt;
        private RejectionReason rejectionReason;
    }

    @Getter
    @Builder
    public static class UserInformationDto {
        private Long userId;
        private String userNickname;
        private String profileImageUrl;

    }

}
