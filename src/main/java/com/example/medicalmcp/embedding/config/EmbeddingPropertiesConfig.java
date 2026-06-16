package com.example.medicalmcp.embedding.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MultiEndpointEmbeddingProperties.class)
public class EmbeddingPropertiesConfig {}
