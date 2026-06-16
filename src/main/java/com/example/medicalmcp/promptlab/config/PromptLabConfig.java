package com.example.medicalmcp.promptlab.config;

import com.example.medicalmcp.promptlab.service.SpecialtyClassificationEvaluator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prompt-lab")
@EnableConfigurationProperties(PromptLabProperties.class)
public class PromptLabConfig {

    @Bean
    SpecialtyClassificationEvaluator specialtyClassificationEvaluator() {
        return new SpecialtyClassificationEvaluator();
    }
}
