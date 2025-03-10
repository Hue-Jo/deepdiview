package community.ddv.component;

import community.ddv.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

  @Value("${spring.jwt.secret}")
  private String secretKey;
  private SecretKey key;


  // 엑세스 토큰 만료시간 : 1시간
  private static final long ACCESS_TOKEN_EXPIRED_TIME = 1000 * 60 * 60;
  // 리프레시 토큰 만료시간 : 15일
  private static final long REFRESH_TOKEN_EXPIRED_TIME = 1000 * 60 * 60 * 24 * 15;
  // 알고리즘
  // private static final SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
//  private SecretKey getSigningKey() {
//    return SIG.HS256.key().build();
//  }
  @PostConstruct
  public void init() {
    if (secretKey == null || secretKey.isEmpty()) {
      throw new IllegalArgumentException("JWT 비밀키가 없습니다.");
    }
    key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
  }


  // 엑세스 토큰 생성
  public String generateAccessToken(String email, Role role) {

    return Jwts.builder()
        .subject(email)
        .claim("role", role.name())
        .issuedAt(new Date()) // 토큰 발행시간
        .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRED_TIME)) // 만료시간
        .signWith(key)
        .compact();
  }

  public Role getRoleFromToken(String token) {

    Claims claims = extractClaims(token);
    return Role.valueOf(claims.get("role", String.class));
  }


  // 리프레시 토큰 생성
  public String generateRefreshToken(String email, Role role) {

    return Jwts.builder()
        .subject(email)
        .claim("role", role.name())
        .issuedAt(new Date()) // 토큰 발행시간
        .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRED_TIME)) // 만료시간
        .signWith(key)
        .compact();
  }


  public Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  // 토큰에서 이메일 추출
  public String extractEmail(String token) {
    return extractClaims(token).getSubject();
  }

  // 토큰 유효성 검사
  public boolean validateToken(String token, String email) {

    try {
      String extractedEmail = extractEmail(token);

      if (!extractedEmail.equals(email)) {
        log.warn("토큰 유효성 검사 실패");
        return false;
      }

      if (isTokenExpired(token)) {
        log.warn("만료된 토큰");
        return false;
      }
      return true;

    } catch (ExpiredJwtException e) {
      log.error("JWT 토큰 검증 실패" + e.getMessage());
      return false;
    }
  }

  // 토큰 만료 여부 확인
  public boolean isTokenExpired(String token) {
    try {
      return extractClaims(token).getExpiration().before(new Date());
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰");
      return true;
    }
  }
}