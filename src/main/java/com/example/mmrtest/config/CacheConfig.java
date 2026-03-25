package com.example.mmrtest.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
@Profile("local")
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "summonerInfo",
                "matchIds",
                "matchRaw",
                "matchSummaryLight",
                "matchDetail",
                "matchTimelineRaw"
        );
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(10000)
                .expireAfterWrite(15, TimeUnit.MINUTES);
    }
}