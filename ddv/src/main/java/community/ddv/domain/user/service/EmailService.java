package community.ddv.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final RedisTemplate<String, String> redisStringTemplate;

  // 인증코드 생성 & 이메일 전송
  public void sendAuthCode(String email) {
    String authCode = createRandomCode();

    redisStringTemplate.opsForValue().set("authCode:" + email, authCode, Duration.ofMinutes(5));

//    SimpleMailMessage message = new SimpleMailMessage();
//    message.setTo(email);
//    message.setSubject("[DeepDiview] 이메일 인증 코드");
//    message.setText("인증 코드 : " + authCode + "\n5분 내로 입력해주세요");

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(email);
      helper.setSubject("[DeepDiview] 이메일 인증 코드");
      helper.setText(buildEmailHTML(authCode), true);
      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException("인증 코드 전송 실패", e);
    }
  }

  private String buildEmailHTML(String authCode) {
    return """
        <div style="width: 100%%; text-align: center; font-family: Arial, sans-serif; padding: 20px;">
          <h2 style="color: #4b4bfc;">[DeepDiview] 이메일 인증 코드</h2>
          <p>아래의 인증 코드를 5분 내에 입력해주세요.</p>
          <div style="display: inline-block; padding: 15px 30px; background-color: #e0e0ff;
                      border: 2px solid #5f5dff; border-radius: 10px; margin-top: 20px;">
            <span style="font-size: 25px; font-weight: bold; color: #4b4bfc;">%s</span>
          </div>
        </div>
        """.formatted(authCode);
  }

  // 인증코드 검증
  public boolean verifyAuthCode(String email, String authCode) {
    String key = "authCode:" + email;
    String savedCode = redisStringTemplate.opsForValue().get(key);
    if (authCode.equals(savedCode)) {
      redisStringTemplate.delete(key);
      redisStringTemplate.opsForValue().set("EMAIL_VERIFIED:" + email, "true", Duration.ofMinutes(10));
      return true;
    }
    return false;
  }

  // 인증 여부 확인 메서드
  public boolean isVerified(String email) {
    return "true".equals(redisStringTemplate.opsForValue().get("EMAIL_VERIFIED:" + email));
  }

  // 인증코드 6자리 난수 생성 메서드
  private String createRandomCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000); // 6자리 난수
    return String.valueOf(code);
  }
}
