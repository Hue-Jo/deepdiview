package community.ddv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DdvApplication {

  public static void main(String[] args) {
    SpringApplication.run(DdvApplication.class, args);
  }

}
