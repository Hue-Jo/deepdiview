package community.ddv.domain.board.repository;

import community.ddv.domain.board.entity.Comment;
import community.ddv.domain.board.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  int countByUser_Id(Long userId);

  // 특정 리뷰에 달린 댓글 조회
  Page<Comment> findByReview(Review review, Pageable pageable);

  // 특정 사용자가 작성한 댓글 조회
  Page<Comment> findByUser_Id(Long userId, Pageable pageable);
}
