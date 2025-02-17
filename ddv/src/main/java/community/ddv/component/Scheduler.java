package community.ddv.component;

import community.ddv.service.MovieApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

  private final MovieApiService movieApiService;

  // 매주 월요일 0시 0분 15초에 영화 데이터 업데이트
  @Scheduled(cron = "15 0 0 * * MON")
  public void updateMovieApi() {
    log.info("영화정보 업데이트를 시작합니다.");
    movieApiService.fetchAndSaveMovies();
    log.info("영화정보 업데이트를 완료했습니다.");
  }

}
