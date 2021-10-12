package no.nav.bidrag.dokument.arkiv;


import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile("live")
public class CacheConfig {

  public static final String STS_SERVICE_USER_TOKEN_CACHE = "STS_SERVICE_USER_TOKEN_CACHE";
  public static final String PERSON_CACHE = "PERSON_CACHE";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.registerCustomCache(STS_SERVICE_USER_TOKEN_CACHE,
        Caffeine.newBuilder()
            .expireAfterWrite(50, TimeUnit.MINUTES)
            .recordStats()
            .build()
    );
    caffeineCacheManager.registerCustomCache(PERSON_CACHE,
        Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build()
    );
    return caffeineCacheManager;
  }
}
