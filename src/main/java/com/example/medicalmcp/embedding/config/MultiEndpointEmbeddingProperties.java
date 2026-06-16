package com.example.medicalmcp.embedding.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "medicalmcp.embedding.multi-endpoint")
public class MultiEndpointEmbeddingProperties {

    @NotEmpty
    @Valid
    private List<EndpointConfig> endpoints = new ArrayList<>();

    @Min(1)
    private int skipDurationMin = 10;

    @Min(1)
    private int workerPerEndpoint = 1;

    @Min(1)
    private int apiBatchSize = 50;

    public List<EndpointConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<EndpointConfig> endpoints) {
        this.endpoints = endpoints;
    }

    public int getSkipDurationMin() {
        return skipDurationMin;
    }

    public void setSkipDurationMin(int skipDurationMin) {
        this.skipDurationMin = skipDurationMin;
    }

    public int getWorkerPerEndpoint() {
        return workerPerEndpoint;
    }

    public void setWorkerPerEndpoint(int workerPerEndpoint) {
        this.workerPerEndpoint = workerPerEndpoint;
    }

    public int getApiBatchSize() {
        return apiBatchSize;
    }

    public void setApiBatchSize(int apiBatchSize) {
        this.apiBatchSize = apiBatchSize;
    }

    public static class EndpointConfig {

        @NotBlank
        private String url;

        private String model;
        private int priority = 0;
        private Integer workers;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public Integer getWorkers() {
            return workers;
        }

        public void setWorkers(Integer workers) {
            this.workers = workers;
        }
    }
}
