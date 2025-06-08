package community.ddv.global.component;

import community.ddv.domain.certification.CertificationService;
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
  private final CertificationService certificationService;

  // 매주 일요일 0시 0분 5초에 영화 데이터 업데이트하면서 인기 영화 목록 캐시 초기화
  @Scheduled(cron = "5 0 0 * * SUN")
  public void updateMovieApi() {
    log.info("영화정보 업데이트를 시작합니다.");
    movieApiService.fetchAndSaveMovies();
    log.info("영화정보 업데이트를 완료했습니다.");
    clearTopMoviesCache();
  }

  public void clearTopMoviesCache() {
    Cache top20Cache = cacheManager.getCache("top20Movies");
    if (top20Cache != null) {
      top20Cache.clear();
    }
    log.info("인기영화 캐시 초기화 완료");
  }

  // 매주 일요일 0시 3분에 런타임 데이터 업데이트
  @Scheduled(cron = "0 3 0 * * SUN")
  public void updateMovieRuntimeApi() {
    log.info("런타임정보 업데이트를 시작합니다.");
    movieApiService.fetchMovieRunTime();
    log.info("런타임정보 업데이트를 완료했습니다.");
  }

  // 매주 일요일 0시 0분 0초 지난 주 1위 영화 캐시 초기화
  @Scheduled(cron = "0 0 0 * * SUN")
  public void clearTopRankMovieCache() {
    Cache topVotedCache = cacheManager.getCache("topRankMovie");

    if (topVotedCache != null) {
      topVotedCache.clear();
    }
    log.info("지난 주 1위 영화 캐시 초기화 완료");
  }

  // 매주 일요일 0시 2초, 인증상태 초기화 스케줄링
  @Scheduled(cron = "2 0 0 * * SUN")
  public void resetCertificationStatus() {
    certificationService.resetCertificationStatus();
    log.info("인증상태 초기화 완료");
  }

}

