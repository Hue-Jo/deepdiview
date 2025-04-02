package community.ddv.global.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 반환
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String jsonResponse = "{\"error\": \"인증이 필요합니다. 엑세스 토큰을 포함해주세요.\"}";
    response.getWriter().write(jsonResponse);
  }
}
