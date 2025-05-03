package community.ddv.global.component;

import community.ddv.domain.user.constant.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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

  public static final String TOKEN_HEADER = "Authorization";
  public static final String TOKEN_PREFIX = "Bearer ";

  @PostConstruct
  public void init() {
    if (secretKey == null || secretKey.isEmpty()) {
      throw new IllegalArgumentException("JWT 비밀키가 없습니다.");
    }
    key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
  }


  // 토큰 생성 메서드
  public String generateToken(String email, Role role, long expirationTime) {
    return Jwts.builder()
        .subject(email)
        .claim("role", role.name())
        .issuedAt(new Date()) // 토큰 발행시간
        .expiration(new Date(System.currentTimeMillis() + expirationTime)) // 만료시간
        .signWith(key)
        .compact();
  }

  // 엑세스 토큰 생성
  public String generateAccessToken(String email, Role role) {
    return generateToken(email, role, ACCESS_TOKEN_EXPIRED_TIME);
  }

  // 리프레시 토큰 생성
  public String generateRefreshToken(String email, Role role) {
    return generateToken(email, role, REFRESH_TOKEN_EXPIRED_TIME);
  }

  // HttpServletRequest(HTTP 요청 정보를 담고 있는 객체)에서 Authorization 헤더 중 Bearer 로 시작하는 JWT 토큰 추출
  public String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(TOKEN_HEADER);
    if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
      // "Bearer " 부분을 제외하고 토큰만 반환
      return bearerToken.substring(TOKEN_PREFIX.length());
    }
    // 토큰이 존재하지 않거나, "Bearer "로 시작하지 않는 경우 null 반환
    return null;
  }

  // 토큰에서 payload(내용) 꺼내기
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

  // 토큰에서 역할 추출
  public Role getRoleFromToken(String token) {
    return Role.valueOf(extractClaims(token).get("role", String.class));
  }

  // 토큰에서 만료시간 추출
  public long getExpirationTimeFromToken(String token) {
    return extractClaims(token).getExpiration().getTime();
  }

  // 토큰에서 사용자 정보를 꺼내어 SpringSecurity가 이해할 수 있는 인증객체(Authentication) 반환
  public Authentication getAuthentication(String token) {
    String email = extractEmail(token);
    Role role = getRoleFromToken(token);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role.name()));
    return new UsernamePasswordAuthenticationToken(email, null, authorities); // // 고유식별자(이메일), 인증정보(jwt에서는 null), authorities(권한)
  }

  // 토큰이 유효한지 검증하는 메서드
  public boolean validateToken(String token) {
    try {
      extractClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      throw e;
    } catch (JwtException e) {
      log.warn("잘못된 JWT 토큰");
      throw e;
    }
  }

  // 토큰 유효성 여부 확인 후 T/F 반환
  public boolean isTokenValid(String token) {
    try {
      Date expirationDate = extractClaims(token).getExpiration();
      return expirationDate != null && expirationDate.after(new Date());
    } catch (JwtException e) {
      return false;
    }
  }
}