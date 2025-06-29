package community.ddv.global.component;

import community.ddv.domain.certification.CertificationService;
import community.ddv.domain.movie.service.MovieApiService;
import community.ddv.domain.notification.NotificationService;
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
  private final NotificationService notificationService;

  // 매주 일요일 0시 0분 5초에 영화 데이터 업데이트하면서 인기 영화 목록 캐시 초기화
  @Scheduled(cron = "5 0 0 * * SUN")
  public void updateMovieApi() {
    log.info("[SCHEDULER] 영화정보 업데이트를 시작");
    movieApiService.fetchAndSaveMovies();
    log.info("[SCHEDULER] 영화정보 업데이트를 완료");
    clearTopMoviesCache();
  }

  public void clearTopMoviesCache() {
    Cache top20Cache = cacheManager.getCache("top20Movies");
    if (top20Cache != null) {
      top20Cache.clear();
    }
    log.info("[SCHEDULER] 인기영화 캐시 초기화 완료");
  }

  // 매주 일요일 0시 3분에 런타임 데이터 업데이트
  @Scheduled(cron = "0 3 0 * * SUN")
  public void updateMovieRuntimeApi() {
    log.info("[SCHEDULER] 런타임정보 업데이트를 시작");
    movieApiService.fetchMovieRunTime();
    log.info("[SCHEDULER] 런타임정보 업데이트를 완료");
  }

  // 매주 일요일 0시 0분 0초 지난 주 1위 영화 캐시 초기화
  @Scheduled(cron = "0 0 0 * * SUN")
  public void clearTopRankMovieCache() {
    Cache topVotedCache = cacheManager.getCache("topRankMovie");

    if (topVotedCache != null) {
      topVotedCache.clear();
    }
    log.info("[SCHEDULER] 지난 주 1위 영화 캐시 초기화 완료");
  }

  // 매주 일요일 0시 2초, 인증상태 초기화 스케줄링
  @Scheduled(cron = "2 0 0 * * SUN")
  public void resetCertificationStatus() {
    log.info("[SCHEDULER] 새로운 주가 됨에 따라 인증상태 초기화 시작");
    certificationService.resetCertificationStatus();
    log.info("[SCHEDULER] 인증 상태 초기화 완료");

  }

  // 매일 새벽 3시, 31일된 알림 삭제 처리 스케줄링
  @Scheduled(cron = "0 0 3 * * *")
  public void deleteOldNotifications() {
    log.info("[SCHEDULER] 31일 이상 지난 알림 삭제 시작");
    notificationService.deleteOldNotifications();
    log.info("[SCHEDULER] 31일 이상 지난 알림 삭제 완료");
  }

}

