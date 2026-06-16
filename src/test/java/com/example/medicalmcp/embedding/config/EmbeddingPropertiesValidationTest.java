package com.example.medicalmcp.embedding.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EmbeddingPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsEmptyEndpointList() {
        MultiEndpointEmbeddingProperties properties = new MultiEndpointEmbeddingProperties();

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void rejectsInvalidEndpointUrl() {
        MultiEndpointEmbeddingProperties properties = new MultiEndpointEmbeddingProperties();
        MultiEndpointEmbeddingProperties.EndpointConfig endpoint = new MultiEndpointEmbeddingProperties.EndpointConfig();
        endpoint.setUrl("not-a-url");
        endpoint.setModel("test-model");
        properties.getEndpoints().add(endpoint);

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void acceptsValidEndpoint() {
        MultiEndpointEmbeddingProperties properties = new MultiEndpointEmbeddingProperties();
        MultiEndpointEmbeddingProperties.EndpointConfig endpoint = new MultiEndpointEmbeddingProperties.EndpointConfig();
        endpoint.setUrl("http://localhost:11434");
        endpoint.setModel("nomic-embed-text:v1.5");
        properties.getEndpoints().add(endpoint);

        Set<?> violations = validator.validate(properties);
        assertThat(violations).isEmpty();
    }
}
