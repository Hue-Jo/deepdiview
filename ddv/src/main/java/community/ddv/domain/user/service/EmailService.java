package community.ddv.domain.user.service;

import community.ddv.domain.user.repository.UserRepository;
import community.ddv.global.exception.DeepdiviewException;
import community.ddv.global.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender mailSender;
  private final RedisTemplate<String, String> redisStringTemplate;
  private final UserRepository userRepository;

  // 인증코드 생성 & 이메일 전송
  public void sendAuthCode(String email) {
    log.info("[EMAIL] 이메일 인증 코드 전송 시작 - email = {}", email);

    if (userRepository.existsByEmail(email)) {
      log.warn("[EMAIL] 이미 가입된 이메일로 인증 요청 - email = {}", email);
      throw new DeepdiviewException(ErrorCode.ALREADY_EXIST_MEMBER);
    }

    String authCode = createRandomCode();

    redisStringTemplate.opsForValue().set("authCode:" + email, authCode, Duration.ofMinutes(5));

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(email);
      helper.setSubject("[DeepDiview] 이메일 인증 코드");
      helper.setText(buildEmailHTML(authCode), true);
      mailSender.send(message);
      log.info("[EMAIL] 인증 코드 이메일 전송 완료 - email = {}", email);
    } catch (MessagingException e) {
      log.error("[EMAIL] 인증 코드 이메일 전송 실패 - email = {}", email);
      throw new RuntimeException("인증 코드 전송 실패", e);
    }
  }

  private String buildEmailHTML(String authCode) {
    return """
        <div style="width: 100%%;  background-color: #141414; color: #ffffff; text-align: center; font-family: Arial, sans-serif; padding: 20px;">
          <h2 style="color: #e50914; font-size: 24px; margin-bottom: 20px;">[DeepDiview] 이메일 인증 코드</h2>
          <p style="font-size: 16px; color: #e0e0e0;">아래 인증 코드를 확인하여 이메일 인증을 완료해주세요.</p>
          <div style="display: inline-block; padding: 18px 36px; background-color: #333333;
                      border: 3px solid #e50914; border-radius: 8px; margin: 30px 0;">
            <span style="font-size: 28px; font-weight: bold; color: #e50914;">%s</span>
          </div>
          <p style="font-size: 14px; color: #e0e0e0;">인증 코드는 5분 내에 입력해주세요.</p>
        </div>
        """.formatted(authCode);
  }

  // 인증코드 6자리 난수 생성 메서드
  private String createRandomCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000); // 6자리 난수
    return String.valueOf(code);
  }

  // 인증코드 검증
  public void verifyAuthCode(String email, String authCode) {
    log.info("[EMAIL] 인증 코드 검증 시작 - email = {}", email);

    String key = "authCode:" + email;
    String savedCode = redisStringTemplate.opsForValue().get(key);

    // 만료된 코드 입력시
    if (savedCode == null) {
      log.warn("[EMAIL] 인증 코드 만료 - email = {}", email);
      throw new DeepdiviewException(ErrorCode.EXPIRED_CODE);
    }
    // 일치하지 않는 코드 입력시
    if (!savedCode.equals(authCode)) {
      log.warn("[EMAIL] 인증 코드 불일치 - email = {}, 입력된 code = {}, 저장된 code = {}", email, authCode, savedCode);
      throw new DeepdiviewException(ErrorCode.INVALID_CODE);
    }

    redisStringTemplate.delete(key);
    redisStringTemplate.opsForValue().set("EMAIL_VERIFIED:" + email, "true", Duration.ofMinutes(10));
    log.info("[EMAIL] 인증 코드 검증 성공 및 이메일 인증 완료 - email = {}", email);

  }

  // 인증 여부 확인 메서드
  public boolean isVerified(String email) {
    return "true".equals(redisStringTemplate.opsForValue().get("EMAIL_VERIFIED:" + email));
  }


}
