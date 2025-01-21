package community.ddv.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String refreshToken;

  @OneToOne(fetch = FetchType.LAZY) // 한 유저는 하나의 리프레시토큰을 가진다.
  @JoinColumn(name = "user_id")
  private User user;

  public void updateRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
