package community.ddv.repository;

import community.ddv.constant.CertificationStatus;
import community.ddv.entity.Certification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

  boolean existsByUser_IdAndStatus(Long userId, CertificationStatus status);
  Page<Certification> findByStatus(CertificationStatus status, Pageable pageable);
}
