package com.example.medicalmcp.support;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import java.time.Duration;

public final class McpSseTestClientFactory {

    private McpSseTestClientFactory() {}

    public static McpSyncClient connect(int port) {
        String baseUrl = "http://localhost:" + port;
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(baseUrl)
                .sseEndpoint("/sse")
                .build();
        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .initializationTimeout(Duration.ofSeconds(30))
                .build();
        client.initialize();
        return client;
    }
}
