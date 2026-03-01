package com.pulsar.diagnostic.web.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats());
        return cacheManager;
    }

    /**
     * Caffeine spec for cluster info cache (short TTL - 10 seconds)
     */
    @Bean
    public Caffeine<Object, Object> clusterInfoCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .recordStats();
    }

    /**
     * Caffeine spec for metrics cache (very short TTL - 5 seconds)
     */
    @Bean
    public Caffeine<Object, Object> metricsCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .recordStats();
    }

    /**
     * Caffeine spec for knowledge search cache (longer TTL - 5 minutes)
     */
    @Bean
    public Caffeine<Object, Object> knowledgeCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }
}