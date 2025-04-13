package community.ddv.global.response;

import community.ddv.domain.board.dto.ReviewResponseDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@AllArgsConstructor
@Getter
public class PageResponse<T> {
  private List<T> content;
  private int number; // 현재 페이지 번호 (0부터)
  private int size; // 페이지 당 아이템 수
  private int totalPages; // 전체 페이지 수
  private long totalElements; // 전체 아이템 수
  private int numberOfElements; // 현재 페이지 내의 아이템 수
  private boolean first; // 첫 페이지 여부
  private boolean last; // 마지막 페이지 여부
  private boolean empty; // 현재 페이지가 비어있는지 여부

  public PageResponse(Page<T> page) {
    this.content = page.getContent();
    this.number = page.getNumber();
    this.size = page.getSize();
    this.totalPages = page.getTotalPages();
    this.totalElements = page.getTotalElements();
    this.numberOfElements = page.getNumberOfElements();
    this.first = page.isFirst();
    this.last = page.isLast();
    this.empty = page.isEmpty();
  }
}
