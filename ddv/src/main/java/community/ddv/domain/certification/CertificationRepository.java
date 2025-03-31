package community.ddv.domain.certification;

import community.ddv.global.constant.CertificationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

  boolean existsByUser_IdAndStatus(Long userId, CertificationStatus status);
  Page<Certification> findByStatus(CertificationStatus status, Pageable pageable);

  // ENUM 타입을 NULL로 초기화 하기 위해 네이티브쿼리 사용
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(value = "UPDATE certification SET status = NULL, rejection_reason = NULL", nativeQuery = true)
  int resetAllCertifications();

  Optional<Certification> findByUser_Id(Long userId);

  Page<Certification> findByStatusIsNotNull(Pageable pageable);
}
