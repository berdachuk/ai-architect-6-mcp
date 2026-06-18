package com.example.medicalmcp;

import com.example.medicalmcp.embedding.service.EmbeddingHealth;
import com.example.medicalmcp.embedding.service.EmbeddingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class MedicalMcpApplication implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(MedicalMcpApplication.class);

    private final Environment environment;

    public MedicalMcpApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(MedicalMcpApplication.class, args);
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        WebServer server = event.getWebServer();
        int port = server.getPort();
        String address = environment.getProperty("server.address", "0.0.0.0");
        boolean sslEnabled = environment.getProperty("server.ssl.enabled", Boolean.class, false);
        String protocol = sslEnabled ? "https" : "http";
        String baseUrl = String.format("%s://%s:%d", protocol, address, port);
        String sseEndpoint = environment.getProperty("spring.ai.mcp.server.sse-endpoint", "/sse");
        String sseMessageEndpoint =
                environment.getProperty("spring.ai.mcp.server.sse-message-endpoint", "/mcp/message");
        String displayHost = "0.0.0.0".equals(address) ? "localhost" : address;

        log.info("");
        log.info("Medical MCP server started");
        log.info("Application URLs:");
        log.info("  SSE endpoint:    {}://{}:{}{}", protocol, displayHost, port, sseEndpoint);
        log.info("  MCP message:     {}://{}:{}{}", protocol, displayHost, port, sseMessageEndpoint);
        log.info("  Health:          {}://{}:{}/actuator/health", protocol, displayHost, port);
        log.info("  Prometheus:      {}://{}:{}/actuator/prometheus", protocol, displayHost, port);
        log.info("  Modulith docs:   {}://{}:{}/actuator/rootModule", protocol, displayHost, port);
        log.info("");
    }

    @Bean
    ApplicationRunner llmHealthLogger(EmbeddingService embeddingService) {
        return args -> {
            List<EmbeddingHealth> results = embeddingService.pingAll();
            int up = (int) results.stream().filter(EmbeddingHealth::isUp).count();
            log.info("");
            log.info("Embedding pool health: {}/{} endpoint(s) UP", up, results.size());
            for (EmbeddingHealth result : results) {
                if (result.isUp()) {
                    log.info(
                            "  UP   {} model={} dim={} latency={}ms",
                            result.url(),
                            result.model(),
                            result.dimensions(),
                            result.latencyMs());
                } else {
                    log.warn("  DOWN {} model={} error={}", result.url(), result.model(), result.error());
                }
            }
            log.info("");
        };
    }
}
