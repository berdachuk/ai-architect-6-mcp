package com.example.medicalmcp.dataset.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = DatasetLoaderPropertiesBindingTest.Config.class)
@TestPropertySource(
        properties = {
            "medicalmcp.dataset.loader.enabled=true",
            "medicalmcp.dataset.loader.batch-size=25",
            "medicalmcp.dataset.loader.sources=classpath:dataset/train-sample-10.csv"
        })
class DatasetLoaderPropertiesBindingTest {

    @Autowired
    private DatasetLoaderProperties datasetLoaderProperties;

    @Test
    void bindsLoaderProperties() {
        assertThat(datasetLoaderProperties.isEnabled()).isTrue();
        assertThat(datasetLoaderProperties.getBatchSize()).isEqualTo(25);
        assertThat(datasetLoaderProperties.getSources()).containsExactly("classpath:dataset/train-sample-10.csv");
    }

    @EnableConfigurationProperties(DatasetLoaderProperties.class)
    static class Config {}
}
