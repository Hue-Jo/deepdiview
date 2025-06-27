package community.ddv.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("DeepDiview")
            .version("1.2.145")
            .description("DeepDiview API 명세서"))
        .addServersItem(new Server().url("https://www.deepdiview.site").description("DeepDiview BE Server"));
  }

}
