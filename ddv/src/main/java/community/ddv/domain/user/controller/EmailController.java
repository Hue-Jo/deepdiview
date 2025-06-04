package community.ddv.domain.user.controller;

import community.ddv.domain.user.dto.EmailDto.EmailRequest;
import community.ddv.domain.user.dto.EmailDto.EmailVerifyRequest;
import community.ddv.domain.user.service.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Tag(name = "Email", description = "이메일 인증 API에 대한 명세를 제공합니다.")
public class EmailController {

  private final EmailService emailService;

  @PostMapping("/send")
  public ResponseEntity<Void> sendCode(
      @RequestBody EmailRequest emailRequest
  ) {
    emailService.sendAuthCode(emailRequest.getEmail());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/verify")
  public ResponseEntity<Void> verifyCode(
      @RequestBody EmailVerifyRequest verifyRequest
  ) {
    emailService.verifyAuthCode(verifyRequest.getEmail(), verifyRequest.getCode());
    return ResponseEntity.ok().build();
  }
}
