package community.ddv.global.component;

import community.ddv.domain.user.constant.Role;
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
import org.springframework.data.redis.core.RedisTemplate;
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
  private final RedisTemplate<String, String> redisStringTemplate;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String token = jwtProvider.extractToken(request);
    try {
      if (token != null) {
        String isBlacked = redisStringTemplate.opsForValue().get(token);
        if ("logout".equals(isBlacked)) {
          log.warn("[TOKEN] 블랙리스트로 등록된 토큰. 접근 거부");
          setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "로그아웃된 토큰입니다.");
          return;
        }

        if (jwtProvider.validateToken(token)) {
          String email = jwtProvider.extractEmail(token);
          Role role = jwtProvider.getRoleFromToken(token);

          UserDetails userDetails = userDetailsService.loadUserByUsername(email);
          Authentication authentication = new UsernamePasswordAuthenticationToken(
              userDetails,
              null,
              Collections.singletonList(new SimpleGrantedAuthority(role.name())));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
      filterChain.doFilter(request, response);

    } catch (ExpiredJwtException e) {
      log.info("[TOKEN] 만료된 JWT 토큰: {}", e.getMessage());
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "토큰이 만료되었습니다.");
    } catch (JwtException e) {
      log.warn("[TOKEN] 잘못된 JWT 토큰: {}", e.getMessage());
      setErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 토큰입니다.");
    }
  }


  private void setErrorResponse(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json; charset=UTF-8"); // JSON + UTF-8 인코딩 설정
    response.getWriter().write("{\"error\": \"" + message + "\"}");
  }
}
