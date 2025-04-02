package community.ddv.global.component;

import community.ddv.global.constant.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

  private final JwtProvider jwtProvider;
  private final UserDetailsService userDetailsService;

  public static final String TOKEN_HEADER = "Authorization";
  public static final String TOKEN_PREFIX = "Bearer ";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String token = extractToken(request);
    try {
      if (token != null) {
        String email = jwtProvider.extractEmail(token);
        jwtProvider.validateToken(token, email);
        Role role = jwtProvider.getRoleFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
            Collections.singletonList(new SimpleGrantedAuthority(role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }

      filterChain.doFilter(request, response);
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰: {}", e.getMessage());
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
    } catch (JwtException e) {
      log.warn("잘못된 JWT 토큰: {}", e.getMessage());
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }
  }

  private String extractToken(HttpServletRequest request) {
    String bearerToken = request.getHeader(TOKEN_HEADER);
    if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
      // "Bearer " 부분을 제외하고 토큰만 반환
      return bearerToken.substring(TOKEN_PREFIX.length());
    }
    // 토큰이 존재하지 않거나, "Bearer "로 시작하지 않는 경우 null 반환
    return null;
  }

  private void setErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json; charset=UTF-8"); // JSON + UTF-8 인코딩 설정
    response.getWriter().write("{\"error\": \"" + message + "\"}");
  }
}
