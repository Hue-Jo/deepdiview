package community.ddv.global.config;

import community.ddv.global.component.JwtAuthenticationEntryPoint;
import community.ddv.global.component.JwtFilter;
import community.ddv.global.component.SseAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private static final String[] AUTH_WHITELIST = {
      "/v3/api-docs/**",
      "/swagger-ui.html",
      "/swagger-resources/**",
      "/swagger-ui/**",
      "/api/users/signup",
      "/api/users/login",
      "/api/emails/**",
      "/api/fetch/genres",
      "/api/fetch/movies",
      "/api/fetch/runtimes",
      "/api/movies/**",
      "/api/discussions/is-sunday",
      "/api/discussions/this-week-movie",
      "/api/votes/result/latest",
      "/api/notifications/subscribe"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity httpSecurity,
      JwtFilter jwtFilter,
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      SseAuthenticationFilter sseAuthenticationFilter)
      throws Exception {

    return httpSecurity
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable) // REST API이므로 CSRF 비활성화
        .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(AUTH_WHITELIST).permitAll() // 로그인 없이도 할 수 있는 기능
            .requestMatchers(HttpMethod.GET, "/api/reviews/{reviewId}").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reviews/{reviewId}/comments").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reviews/movie/{tmdbId}").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/reviews/latest").permitAll()
            .anyRequest().authenticated()
        )
        .exceptionHandling(exception ->
            exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)) // 401 처리
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(sseAuthenticationFilter, JwtFilter.class) // SSE 인증 필터
        .build();
  }

  // CORS 설정
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowCredentials(true); // 자격증명 허용
    configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://deepdiview.vercel.app"));
    configuration.addAllowedHeader("*");
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedMethod("PUT");
    configuration.addAllowedMethod("DELETE");

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);// 모든 경로
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration)
      throws Exception {
    return authenticationConfiguration.getAuthenticationManager();

  }
}
