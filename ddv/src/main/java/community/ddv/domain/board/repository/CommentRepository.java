package community.ddv.domain.board.repository;

import community.ddv.domain.board.entity.Comment;
import community.ddv.domain.board.entity.Review;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  int countByUser_Id(Long userId);

  int countByReview(Review review);

  // 특정 리뷰에 달린 댓글 조회
  //Page<Comment> findByReview(Review review, Pageable pageable);

  // 커서 없는 첫 페이지
  List<Comment> findByReviewOrderByCreatedAtDescIdDesc(Review review, Pageable pageable);

  // 주어진 커서 (createdAt, id) 이전의 댓글들만 조회
  @Query("""
    SELECT c
    FROM Comment c
    WHERE c.review = :review
      AND ((c.createdAt < :createdAt) OR (c.createdAt = :createdAt AND c.id < :id))
    ORDER BY c.createdAt DESC, c.id DESC
  """)
  List<Comment> findCommentsByReviewBeforeCursor(
      @Param("review") Review review,
      @Param("createdAt") LocalDateTime createdAt,
      @Param("id") Long id,
      Pageable pageable
  );


  // 특정 사용자가 작성한 댓글 조회
  Page<Comment> findByUser_Id(Long userId, Pageable pageable);
}
