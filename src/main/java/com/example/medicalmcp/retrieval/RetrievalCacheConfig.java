package com.example.medicalmcp.retrieval;

import com.example.medicalmcp.retrieval.config.RetrievalProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RetrievalCacheConfig {

    @Bean
    CacheManager cacheManager(RetrievalProperties retrievalProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager("datasetStats");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(retrievalProperties.getStatsCacheTtlSeconds(), TimeUnit.SECONDS));
        return manager;
    }
}
