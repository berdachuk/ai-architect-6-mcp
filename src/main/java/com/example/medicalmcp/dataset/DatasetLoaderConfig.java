package com.example.medicalmcp.dataset;

import com.example.medicalmcp.dataset.config.DatasetLoaderProperties;
import com.example.medicalmcp.dataset.service.DatasetLoaderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DatasetLoaderProperties.class)
public class DatasetLoaderConfig {

    @Bean
    CommandLineRunner datasetLoaderRunner(DatasetLoaderService loader, DatasetLoaderProperties properties) {
        return args -> {
            if (properties.isEnabled()) {
                loader.loadIfEmpty();
            }
        };
    }
}
