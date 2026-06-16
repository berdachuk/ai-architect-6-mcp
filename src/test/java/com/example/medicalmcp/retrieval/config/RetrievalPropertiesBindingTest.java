package com.example.medicalmcp.retrieval.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = RetrievalPropertiesBindingTest.Config.class)
@TestPropertySource(
        properties = {
            "medicalmcp.retrieval.max-limit=40",
            "medicalmcp.retrieval.default-top-k=7",
            "medicalmcp.retrieval.similarity-threshold=0.65",
            "medicalmcp.retrieval.stats-cache-ttl-seconds=30"
        })
class RetrievalPropertiesBindingTest {

    @Autowired
    private RetrievalProperties retrievalProperties;

    @Test
    void bindsRetrievalProperties() {
        assertThat(retrievalProperties.getMaxLimit()).isEqualTo(40);
        assertThat(retrievalProperties.getDefaultTopK()).isEqualTo(7);
        assertThat(retrievalProperties.getSimilarityThreshold()).isEqualTo(0.65);
        assertThat(retrievalProperties.getStatsCacheTtlSeconds()).isEqualTo(30);
    }

    @EnableConfigurationProperties(RetrievalProperties.class)
    static class Config {}
}
