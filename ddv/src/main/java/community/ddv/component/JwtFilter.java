package community.ddv.component;

import community.ddv.constant.Role;
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
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = extractToken(request);

    // jwt 토큰이 있고 유효한 경우, 인증 처리
    if (token != null && jwtProvider.validateToken(token, jwtProvider.extractEmail(token))) {
      String email = jwtProvider.extractEmail(token);
      Role role = jwtProvider.getRoleFromToken(token);
      UserDetails userDetails = userDetailsService.loadUserByUsername(email);
      Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
          Collections.singletonList(new SimpleGrantedAuthority(role.name())));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.info("인증된 유저. 토큰 : {}", token);
    }
//    else {
//      log.error("유효하지 않은 토큰");
//    }
    filterChain.doFilter(request, response);
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
}
