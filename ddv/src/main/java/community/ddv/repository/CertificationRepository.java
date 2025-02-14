package community.ddv.repository;

import community.ddv.constant.CertificationStatus;
import community.ddv.entity.Certification;
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

  // 특정 영화에 대한 인증
  // boolean existsByUser_IdANdMovie_IdAndStatus(Long userId, Long movieId);

  @Modifying
  @Query("UPDATE Certification c SET c.status = NULL")
  int resetAllCertifications();

  Optional<Certification> findByUser_Id(Long userId);
}
