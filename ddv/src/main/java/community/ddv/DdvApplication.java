package community.ddv;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class DdvApplication {

  public static void main(String[] args) {
    SpringApplication.run(DdvApplication.class, args);
  }

  @PostConstruct
  public void changeTimeKST() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
  }
}
