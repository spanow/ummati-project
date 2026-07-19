package orga.takwa.ummati.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * T-144 — Performance : configuration fine du cache Caffeine par cache nommé.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caches disponibles :
     * - "skills"         : liste des compétences (30 min, 200 entries)
     * - "org-stats"      : stats d'une organisation (5 min, 500 entries)
     * - "dashboard-vol"  : données dashboard bénévole (2 min, 1000 entries)
     * - "admin-stats"    : stats admin plateforme (5 min, 10 entries)
     * - "public-stats"   : stats publiques landing page (10 min, 1 entry)
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("skills",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(30, TimeUnit.MINUTES).build());
        manager.registerCustomCache("org-stats",
                Caffeine.newBuilder().maximumSize(500).expireAfterWrite(5, TimeUnit.MINUTES).build());
        manager.registerCustomCache("dashboard-vol",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(2, TimeUnit.MINUTES).build());
        manager.registerCustomCache("admin-stats",
                Caffeine.newBuilder().maximumSize(10).expireAfterWrite(5, TimeUnit.MINUTES).build());
        manager.registerCustomCache("public-stats",
                Caffeine.newBuilder().maximumSize(1).expireAfterWrite(10, TimeUnit.MINUTES).build());
        // Default
        manager.setCaffeine(
                Caffeine.newBuilder().maximumSize(500).expireAfterWrite(10, TimeUnit.MINUTES));
        return manager;
    }
}

