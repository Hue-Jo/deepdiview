package community.ddv.config;

import community.ddv.component.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
      "/api/fetch-genres",
      "/api/fetch-movies,"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, JwtFilter jwtFilter)
      throws Exception {

    return httpSecurity
        .csrf(AbstractHttpConfigurer::disable) // REST API이므로 CSRF 비활성화
        .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(AUTH_WHITELIST)
            .permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // jwt 필터
        .build();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration)
      throws Exception {
    return authenticationConfiguration.getAuthenticationManager();

  }
}
