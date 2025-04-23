package community.ddv.global.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@AllArgsConstructor
public class SseAuthenticationFilter extends OncePerRequestFilter {
  private final JwtProvider jwtProvider;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);

    if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {

      String token = extractTokenForSse(request);

      if (token == null || !jwtProvider.validateToken(token)) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return;
      }

      Authentication authentication = jwtProvider.getAuthentication(token);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String extractTokenForSse(HttpServletRequest request) {
    String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // "Bearer " 이후 토큰 부분만 추출
    }
    return null;
  }
}