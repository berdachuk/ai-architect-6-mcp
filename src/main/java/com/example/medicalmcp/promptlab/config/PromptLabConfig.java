package com.example.medicalmcp.promptlab.config;

import com.example.medicalmcp.promptlab.service.SpecialtyClassificationEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Bean("promptLabChatModel")
    @ConditionalOnProperty(prefix = "medicalmcp.prompt-lab.chat", name = "enabled", havingValue = "true")
    ChatModel promptLabChatModel(PromptLabProperties properties) {
        PromptLabProperties.Chat chat = properties.getChat();
        String baseUrl = normalizeOpenAiBaseUrl(chat.getBaseUrl());
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey("ollama")
                        .model(chat.getModel())
                        .temperature(0.0)
                        .build())
                .build();
    }

    private static String normalizeOpenAiBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }
}
