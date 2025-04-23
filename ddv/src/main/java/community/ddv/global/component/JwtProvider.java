package community.ddv.global.component;

import community.ddv.domain.user.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

  // 토큰 파싱해서 Authentication 객체 반환
  public Authentication getAuthentication(String token) {
    String email = extractEmail(token);
    Role role = getRoleFromToken(token);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.name()));
    return new UsernamePasswordAuthenticationToken(email, token, authorities);
  }

  // 토큰 유효성 검사 (1)
  public void validateToken(String token, String email) {

    try {
      String extractedEmail = extractEmail(token);
      if (!extractedEmail.equals(email)) {
        throw new JwtException("유효하지 않은 토큰입니다.");
      }

      if (isTokenExpired(token)) {
        throw new ExpiredJwtException(null, null, "만료된 토큰입니다.");
      }
    } catch (ExpiredJwtException e) {
      log.warn("JWT 토큰 만료" + e.getMessage());
      throw e;
    } catch (JwtException e) {
      log.warn("JWT 토큰 검증 실패" + e.getMessage());
      throw e;
    }
  }

  // 토큰 유효성 검사 (2)
  public boolean validateToken(String token) {
    try {
      return !isTokenExpired(token);
    } catch (JwtException e) {
      log.warn("JWT 토큰 검증 실패: " + e.getMessage());
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