package community.ddv.global.component;

import community.ddv.domain.movie.service.MovieApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

  private final MovieApiService movieApiService;
  private final CacheManager cacheManager;

  // 매주 월요일 0시 0분 15초에 영화 데이터 업데이트하면서 인기 영화 목록 캐시 초기화
  @Scheduled(cron = "15 0 0 * * MON")
  public void updateMovieApi() {
    log.info("영화정보 업데이트를 시작합니다.");
    movieApiService.fetchAndSaveMovies();
    log.info("영화정보 업데이트를 완료했습니다.");

    clearTopMoviesCache();
  }

  private void clearTopMoviesCache() {
    Cache top20Cache = cacheManager.getCache("top20Movies");

    if (top20Cache != null) {
      top20Cache.clear();
    }
    log.info("인기영화 캐시 초기화 완료");
  }

//
//  @Scheduled(cron = "0 0 0 * * *")
//  @CacheEvict(value = "topRankMovie", key = "'last_week_top_movie'")
//  public void evictTopRankMovie() {
//    log.info("투표결과 캐시 무효화 완료");
//  }

}

