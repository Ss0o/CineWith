package com.community.board.movie.discovery;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
@Configuration(proxyBeanMethods=false) @EnableCaching
public class MovieDiscoveryCacheConfiguration {
 @Bean CacheManager cacheManager() { CaffeineCacheManager manager=new CaffeineCacheManager("movieDiscoveryHome"); manager.setCaffeine(Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofHours(3))); return manager; }
}
