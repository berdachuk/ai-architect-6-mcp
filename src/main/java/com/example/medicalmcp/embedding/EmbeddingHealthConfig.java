package com.example.medicalmcp.embedding;

import com.example.medicalmcp.embedding.multiendpoint.EmbeddingEndpointPool;
import com.example.medicalmcp.embedding.multiendpoint.EndpointState;
import com.example.medicalmcp.embedding.service.EmbeddingHealth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingHealthConfig {

    @Bean
    HealthContributor embeddingHealthContributor(EmbeddingEndpointPool pool) {
        List<EndpointState> endpoints = pool.getEndpoints();
        if (endpoints.isEmpty()) {
            return new NoEndpointHealthIndicator();
        }
        Map<String, HealthContributor> contributors = new LinkedHashMap<>();
        for (int i = 0; i < endpoints.size(); i++) {
            EndpointState endpoint = endpoints.get(i);
            String name = "endpoint-" + i + "-" + safeName(endpoint.getModel());
            contributors.put(name, new EndpointHealthIndicator(endpoint));
        }
        return CompositeHealthContributor.fromMap(contributors);
    }

    private static String safeName(String model) {
        return model == null ? "unknown" : model.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private static final class NoEndpointHealthIndicator extends AbstractHealthIndicator {

        @Override
        protected void doHealthCheck(org.springframework.boot.health.contributor.Health.Builder builder) {
            builder.down().withDetail("reason", "No embedding endpoints configured");
        }
    }

    private static final class EndpointHealthIndicator extends AbstractHealthIndicator {

        private final EndpointState endpoint;

        EndpointHealthIndicator(EndpointState endpoint) {
            super("Embedding endpoint probe failed");
            this.endpoint = endpoint;
        }

        @Override
        protected void doHealthCheck(org.springframework.boot.health.contributor.Health.Builder builder) {
            EmbeddingHealth health = endpoint.ping();
            if (health.isUp()) {
                builder.up()
                        .withDetail("url", health.url())
                        .withDetail("model", health.model())
                        .withDetail("dimensions", health.dimensions())
                        .withDetail("latencyMs", health.latencyMs());
            } else {
                builder.down()
                        .withDetail("url", health.url())
                        .withDetail("model", health.model())
                        .withDetail("error", health.error());
            }
        }
    }
}
