package community.ddv.domain.user.service;

import java.time.Duration;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setSubject("[DeepDiview] 이메일 인증 코드");
    message.setText("인증 코드 : " + authCode + "\n5분 내로 입력해주세요");
    mailSender.send(message);

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

  public boolean isVerified(String email) {
    return "true".equals(redisStringTemplate.opsForValue().get("EMAIL_VERIFIED:" + email));
  }

  private String createRandomCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000); // 6자리 난수
    return String.valueOf(code);
  }
}
