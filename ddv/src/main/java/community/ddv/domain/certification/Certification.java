package community.ddv.domain.certification;

import community.ddv.domain.certification.constant.CertificationStatus;
import community.ddv.domain.certification.constant.RejectionReason;
import community.ddv.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Certification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  private String certificationUrl; // 인증샷 url

  @Enumerated(EnumType.STRING)
  private CertificationStatus status; // 인증 여부 (보류/승인/거절)

  @Enumerated(EnumType.STRING)
  private RejectionReason rejectionReason; // 인증 거절 사유

  private LocalDateTime createdAt; // 인증 제출 시각

  public void setStatus(CertificationStatus status, RejectionReason rejectionReason) {
    this.status = status;
    this.rejectionReason = rejectionReason;
  }

  public void updateCertification(String newUrl) {
    this.certificationUrl = newUrl;
    this.status = CertificationStatus.PENDING;
    this.rejectionReason = null;
    this.createdAt = LocalDateTime.now();
  }


}
